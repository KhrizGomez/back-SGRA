package com.CLMTZ.Backend.dto.academic;

import lombok.Data;

@Data
public class CareerLoadDTO {
    private String nombreArea;       // Ej: "Facultad de Ciencias de la Ingeniería"
    private String abrevArea;        // Ej: "FCI"
    private String nombreModalidad;  // Ej: "Presencial"
    private String nombreCarrera;    // Ej: "Ingeniería de Software"
    private Short semestres;         // Ej: 8
}