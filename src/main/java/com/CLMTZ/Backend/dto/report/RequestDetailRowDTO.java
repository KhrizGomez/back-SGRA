package com.CLMTZ.Backend.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestDetailRowDTO {
    private Integer requestId;
    private String createdAt;
    private String studentName;
    private String subjectName;
    private String teacherName;
    private String sessionType;
    private String statusName;
    private String reason;
}
