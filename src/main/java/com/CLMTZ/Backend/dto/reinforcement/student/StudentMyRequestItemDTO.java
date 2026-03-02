package com.CLMTZ.Backend.dto.reinforcement.student;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentMyRequestItemDTO {
    private Integer requestId;
    private String requestDateTime;
    private String subjectCode;
    private String subjectName;
    private String topic;
    private String teacherName;
    private String sessionType;
    private String status;
}