package com.CLMTZ.Backend.dto.academic;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TeachingDTO {
    private Integer teachingId;
    private Boolean state;
    private Integer userId;
    private Integer modalityId;
    // Datos Personales (usados en API GET)
    private String cedula;
    private String nombres;
    private String apellidos;
    private String correo;
    private String telefono;

    // Datos del Excel Docente.xls
    private String coordinacionTexto; // Col 0: COORDINACIÓN (facultad/área)
    private String carreraTexto;      // Col 1: CARRERA
    private String nivelTexto;        // Col 2: NIVEL (ej. "1ER NIVEL")
    private String asignaturaTexto;   // Col 3: MATERIA
    private String paraleloTexto;     // Col 4: PARALELO (ej. "B")
    private String nombreCompleto;    // Col 5: PROFESOR (apellidos + nombres combinado)
}
