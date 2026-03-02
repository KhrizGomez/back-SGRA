package com.CLMTZ.Backend.dto.reinforcement.student;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO simplificado para crear solicitud de refuerzo.
 * El estudiante solo elige: asignatura, tipo de sesión, motivo y archivos.
 * El docente y periodo se resuelven automáticamente en el backend.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentRequestCreateRequestDTO {
    private Integer subjectId;          // Asignatura seleccionada por el estudiante
    private Integer sessionTypeId;      // Tipo de sesión (Individual / Grupal)
    private String reason;              // Motivo de la solicitud
    private List<Integer> participantIds;  // IDs de compañeros para sesiones grupales (opcional)
}