package com.CLMTZ.Backend.dto.academic;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClassScheduleDetailDTO {
    // Horario de clase
    private Integer idClassSchedule;
    private Short day;
    private Boolean active;

    // Franja horaria
    private Integer timeSlotId;
    private LocalTime startTime;
    private LocalTime endTime;

    // Clase asignada
    private Integer classId;

    // Docente
    private Integer teachingId;
    private String teacherFirstName;
    private String teacherLastName;
    private String teacherIdentification;

    // Asignatura
    private Integer subjectId;
    private String subjectName;
    private Short semester;

    // Paralelo
    private Integer parallelId;
    private String section;

    // Periodo
    private Integer periodId;
    private String period;
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
}
