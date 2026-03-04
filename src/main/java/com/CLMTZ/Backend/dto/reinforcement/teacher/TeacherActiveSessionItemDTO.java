package com.CLMTZ.Backend.dto.reinforcement.teacher;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeacherActiveSessionItemDTO {
    private Integer scheduledId;
    private String subjectName;
    private String scheduledDate;
    private String startTime;
    private String endTime;
    private String modality;
    private String estimatedDuration;
    private String statusName;
    private String sessionType;
    private Integer participantCount;
    private String virtualLink;  // null if presencial
}
