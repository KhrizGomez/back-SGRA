package com.CLMTZ.Backend.dto.reinforcement;

import java.time.LocalDateTime;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScheduledReinforcementDTO {
    private Integer scheduledReinforcementId;
    private LocalTime estimatedTime;
    private String reason;
    private LocalDateTime newSchedule;
    private String state;
    private Integer sessionTypeId;
    private Integer modalityId;
    private Integer timeSlotId;
}
