package com.CLMTZ.Backend.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIValidationIssue {

    public enum Severity {
        /** Error crítico: la fila no puede procesarse */
        ERROR,
        /** Advertencia: la fila puede procesarse pero tiene datos sospechosos */
        WARNING,
        /** Información: sugerencia de mejora */
        INFO
    }

    /** Número de fila en el Excel (1-based, sin contar encabezado) */
    private int row;

    /** Nombre del campo/columna afectada */
    private String field;

    /** Severidad del problema */
    private Severity severity;

    /** Descripción del problema detectado */
    private String message;

    /** Sugerencia de corrección (puede ser null) */
    private String suggestion;

    /** Origen de la validación: "AI" o "FALLBACK" */
    private String source;
}

