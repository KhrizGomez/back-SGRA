package com.CLMTZ.Backend.dto.academic;

import java.time.LocalTime;

import lombok.Data;

@Data
public class ClassScheduleLoadDTO {
    private String cedulaDocente;     // Ej: "0987654321"
    private String nombreAsignatura;  // Ej: "Matemáticas"
    private String nombreParalelo;    // Ej: "A"
    private String nombrePeriodo;     // Ej: "2026-CI"
    private Integer diaSemana;        // Ej: 1 (Lunes), 2 (Martes)...
    private LocalTime horaInicio;     // Ej: 08:00
    private LocalTime horaFin;        // Ej: 10:00
}