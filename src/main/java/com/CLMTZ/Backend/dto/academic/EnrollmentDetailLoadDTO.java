package com.CLMTZ.Backend.dto.academic;

import lombok.Data;

@Data
public class EnrollmentDetailLoadDTO {
    private String identificador;  // Col 1: cédula / pasaporte / código postal
    private String sexo;            // Col 2: sexo (M / F / ...)
    private String asignatura;      // Col 3: nombre de la asignatura
    private Integer semestre;       // Col 4: nivel / semestre
    private String paralelo;        // Col 5: paralelo
}
