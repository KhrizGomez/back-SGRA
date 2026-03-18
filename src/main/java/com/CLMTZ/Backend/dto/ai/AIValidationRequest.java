package com.CLMTZ.Backend.dto.ai;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIValidationRequest {

    /** Tipo de carga: "students", "teachers", "class_schedules", "careers", etc. */
    private String loadType;

    /** Filas del Excel representadas como mapas clave-valor (nombre columna -> valor) */
    private List<Map<String, Object>> rows;

    /** Reglas de negocio específicas del SP que se usará (campos obligatorios, formatos, rangos) */
    private List<String> businessRules;

    /** Nombres de las columnas esperadas */
    private List<String> expectedColumns;

    /**
     * Contexto de la base de datos en formato JSON string.
     * Inyectado por ExcelValidationContextService antes de llamar a la IA.
     * Ejemplo para "teachers": {"carreras":["Ingeniería en Software",...], "materias":[...]}
     */
    private String dbContext;
}

