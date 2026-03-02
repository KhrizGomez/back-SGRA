package com.CLMTZ.Backend.dto.ai;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta unificada para los endpoints de carga de datos.
 * Combina la validación previa (IA/fallback) con el resultado de los SPs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataLoadResponse {

    /** Resultado de la validación previa (IA o fallback) */
    private AIValidationResult preValidation;

    /** Resultado de la ejecución de SPs (reporte fila por fila) — null si se detuvo por errores críticos */
    private List<String> loadResult;

    /** Indica si la carga fue ejecutada o se detuvo por validación */
    private boolean loadExecuted;

    /** Mensaje general del proceso */
    private String message;
}

