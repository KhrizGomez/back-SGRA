package com.CLMTZ.Backend.service.academic.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.CLMTZ.Backend.dto.ai.AIValidationRequest;
import com.CLMTZ.Backend.dto.ai.AIValidationResult;
import com.CLMTZ.Backend.service.academic.IExcelValidationService;
import com.CLMTZ.Backend.service.ai.ExcelAIValidationService;
import com.CLMTZ.Backend.service.ai.ExcelValidationContextService;
import com.CLMTZ.Backend.util.ExcelHelper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelValidationServiceImpl implements IExcelValidationService {

    private final ExcelAIValidationService aiValidationService;
    private final ExcelValidationContextService validationContextService;

    @Override
    public AIValidationResult validateExcel(MultipartFile file, String loadType) {
        if (!ExcelHelper.hasExcelFormat(file)) {
            return AIValidationResult.builder()
                    .aiValidated(false)
                    .recommendedAction("REJECT")
                    .summary("El archivo no es un Excel válido (.xls o .xlsx)")
                    .build();
        }

        try {
            List<Map<String, Object>> rows = ExcelHelper.excelToGenericMap(file.getInputStream(), loadType);

            if (rows.isEmpty()) {
                return AIValidationResult.builder()
                        .aiValidated(false)
                        .recommendedAction("REJECT")
                        .summary("El archivo está vacío o no tiene datos válidos para procesar.")
                        .build();
            }

            // Obtener contexto referencial de la BD
            String dbContext = null;
            try {
                dbContext = validationContextService.getContextForType(loadType);
            } catch (Exception ex) {
                log.warn("[VALIDATE-EXCEL] No se pudo obtener contexto BD: {}", ex.getMessage());
            }

            AIValidationRequest request = AIValidationRequest.builder()
                    .loadType(loadType)
                    .rows(rows)
                    .dbContext(dbContext)
                    .build();

            return aiValidationService.validate(request);

        } catch (Exception e) {
            log.error("[VALIDATE-EXCEL] Error al procesar archivo: {}", e.getMessage(), e);
            return AIValidationResult.builder()
                    .aiValidated(false)
                    .recommendedAction("REJECT")
                    .summary("Error al procesar el archivo: " + e.getMessage())
                    .build();
        }
    }
}
