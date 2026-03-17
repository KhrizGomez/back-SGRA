package com.CLMTZ.Backend.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Petición para el asistente de creación de solicitudes (Feature 1).
 * El estudiante envía el nombre de la asignatura y describe su problema en texto libre.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentAISuggestRequestDTO {

    /** ID de la asignatura seleccionada (para referencia futura, no usado por la IA directamente) */
    private Integer subjectId;

    /** Nombre de la asignatura, inyectado en el prompt */
    private String subjectName;

    /** Descripción libre del problema del estudiante */
    private String problemDescription;
}