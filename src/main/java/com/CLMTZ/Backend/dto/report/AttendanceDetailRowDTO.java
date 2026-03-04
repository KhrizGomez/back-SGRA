package com.CLMTZ.Backend.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceDetailRowDTO {
    private Integer sessionId;
    private String studentName;
    private String subjectName;
    private String teacherName;
    private String sessionDate;
    private String sessionType;
    private Boolean attended;
    private String duration;
    private String notes;
}
