package com.CLMTZ.Backend.dto.reinforcement.teacher;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeacherRequestItemDTO {
    private Integer requestId;
    private String studentName;
    private String subjectName;
    private String sessionType;
    private String reason;
    private String statusName;
    private Integer statusId;
    private String createdAt;
    private Boolean isGroupal;
    private Integer participantCount;
}
