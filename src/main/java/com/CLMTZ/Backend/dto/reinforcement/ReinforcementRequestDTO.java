package com.CLMTZ.Backend.dto.reinforcement;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReinforcementRequestDTO {
    private Integer reinforcementRequestId;
    private Short requestedDay;
    private String reason;
    private String fileUrl;
    private LocalDateTime createdAt;
    private Integer studentId;
    private Integer teacherId;
    private Integer topicId;
    private Integer timeSlotId;
    private Integer modalityId;
    private Integer sessionTypeId;
    private Integer requestStatusId;
    private Integer periodId;
}
