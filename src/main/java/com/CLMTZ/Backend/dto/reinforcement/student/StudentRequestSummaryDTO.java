package com.CLMTZ.Backend.dto.reinforcement.student;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentRequestSummaryDTO {
    private Integer requestId;
    private String studentName;
    private String studentEmail;
    private String teacherName;
    private String teacherEmail;
    private String subjectName;
    private String courseName;
    private String parallelName;
    private String reason;
}

