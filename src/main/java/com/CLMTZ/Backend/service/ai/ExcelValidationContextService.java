package com.CLMTZ.Backend.service.ai;

import com.CLMTZ.Backend.repository.ai.ExcelValidationContextRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelValidationContextService {

    private final ExcelValidationContextRepository contextRepository;

    /**
     * Retorna el contexto de la BD como JSON string para inyectarlo al prompt de validación.
     * Cada tipo de carga tiene su propia función PostgreSQL que sabe qué datos son relevantes.
     *
     * @param loadType tipo de carga: "teachers", "registrations", "class_schedules"
     * @return JSON string con el contexto referencial, o null si no aplica / falla
     */
    public String getContextForType(String loadType) {
        try {
            return switch (loadType) {
                case "teachers"         -> contextRepository.getTeachersContext();
                case "registrations"    -> contextRepository.getRegistrationsContext();
                case "class_schedules"  -> contextRepository.getClassSchedulesContext();
                default                 -> null;
            };
        } catch (Exception e) {
            log.warn("[ExcelValidationContextService] No se pudo obtener contexto para tipo '{}': {}", loadType, e.getMessage());
            return null;
        }
    }
}
