package com.CLMTZ.Backend.controller.academic;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.CLMTZ.Backend.dto.ai.AIValidationResult;
import com.CLMTZ.Backend.service.academic.IExcelValidationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/academic/excel-validations")
@RequiredArgsConstructor
public class ExcelValidationController {

    private final IExcelValidationService excelValidationService;

    /**
     * Valida un archivo Excel usando IA antes de subirlo oficialmente.
     * Analiza campos vacíos, duplicados, formatos incorrectos, idioma, etc.
     */
    @PostMapping("/validate")
    public ResponseEntity<AIValidationResult> validateExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam("loadType") String loadType) {

        AIValidationResult result = excelValidationService.validateExcel(file, loadType);
        return ResponseEntity.ok(result);
    }
}
