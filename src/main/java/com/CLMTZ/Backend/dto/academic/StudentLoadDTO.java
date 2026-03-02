package com.CLMTZ.Backend.dto.academic;

import lombok.Data;

@Data
public class StudentLoadDTO {
    private String identificacion; // cédula, pasaporte o código postal
    private String nombres;
    private String apellidos;
    private String correo;
    private String telefono;

    // Datos académicos (parámetros del endpoint, no del Excel)
    private String carreraTexto;
    private String modalidadTexto;
}
