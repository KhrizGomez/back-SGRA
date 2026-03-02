package com.CLMTZ.Backend.dto.academic;

import java.time.LocalDate;

import lombok.Data;

@Data
public class PeriodLoadDTO {
    private String nombrePeriodo; // Ej: "2026-CI"
    private LocalDate fechaInicio; // Ej: 2026-05-01
    private LocalDate fechaFin;    // Ej: 2026-09-30
}