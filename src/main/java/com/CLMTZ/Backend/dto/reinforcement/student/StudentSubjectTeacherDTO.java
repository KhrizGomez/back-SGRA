package com.CLMTZ.Backend.dto.reinforcement.student;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para el docente asignado al paralelo del estudiante en una asignatura.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentSubjectTeacherDTO {
    private Integer teacherId;
    private String fullName;
    private String email;
}

