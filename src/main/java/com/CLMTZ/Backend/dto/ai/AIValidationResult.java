package com.CLMTZ.Backend.dto.ai;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIValidationResult {

    /** Lista de problemas detectados */
    @Builder.Default
    private List<AIValidationIssue> issues = new ArrayList<>();

    /** Indica si la validación fue realizada por IA (true) o por fallback Java (false) */
    private boolean aiValidated;

    /** Acción recomendada: "PROCEED", "REVIEW", "REJECT" */
    private String recommendedAction;

    /** Resumen general de la validación */
    private String summary;

    /** Tiempo en milisegundos que tomó la validación */
    private long validationTimeMs;

    /** Retorna true si hay al menos un issue con severidad ERROR */
    public boolean hasCriticalErrors() {
        return issues.stream()
                .anyMatch(i -> i.getSeverity() == AIValidationIssue.Severity.ERROR);
    }

    /** Cuenta issues por severidad */
    public long countBySeverity(AIValidationIssue.Severity severity) {
        return issues.stream()
                .filter(i -> i.getSeverity() == severity)
                .count();
    }
}

