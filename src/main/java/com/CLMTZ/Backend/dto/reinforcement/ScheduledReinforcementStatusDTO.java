package com.CLMTZ.Backend.dto.reinforcement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScheduledReinforcementStatusDTO {
    private Integer scheduledReinforcementStatusId;
    private String scheduledReinforcementStatus;
    private Boolean state;
}
