package com.CLMTZ.Backend.dto.reinforcement.student;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentHistorySessionItemDTO {
    private Integer completedSessionId;
    private Boolean attended;
    private String duration;
    private String notes;
    private Integer requestId;
    private String requestDateTime;
    private String subjectName;
    private String syllabusName;
    private Short unit;
    private String teacherName;
    private String sessionType;
}