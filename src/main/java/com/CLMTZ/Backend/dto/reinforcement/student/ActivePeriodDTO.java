package com.CLMTZ.Backend.dto.reinforcement.student;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para el periodo académico activo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivePeriodDTO {
    private Integer periodId;
    private String periodName;
}

