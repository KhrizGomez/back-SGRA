package com.CLMTZ.Backend.dto.reinforcement.student;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentHistoryRequestItemDTO {
    private Integer requestId;
    private String createdAt;
    private String subjectName;
    private String syllabusName;
    private Short unit;
    private String teacherName;
    private String sessionType;
    private Integer statusId;
    private String statusName;
    private String reason;
    private Short requestedDay;
}