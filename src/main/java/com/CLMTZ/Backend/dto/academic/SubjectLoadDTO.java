package com.CLMTZ.Backend.dto.academic;

import lombok.Data;

@Data
public class SubjectLoadDTO {
    private String nombreCarrera;     // Ej: "Ingeniería de Software"
    private String nombreAsignatura;  // Ej: "Programación Orientada a Objetos"
    private Short semestre;           // Ej: 3
}
