package com.CLMTZ.Backend.dto.reinforcement.teacher;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeacherSessionHistoryItemDTO {
    private Integer scheduledId;
    private String subjectName;
    private String scheduledDate;
    private String modality;
    private String estimatedDuration;
    private String timeSlot;
    private String statusName;
    private String sessionType;
    private Integer studentCount;
}
