package com.CLMTZ.Backend.dto.reinforcement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScheduledReinforcementDetailDTO {
    private Integer scheduledReinforcementDetailId;
    private Boolean state;
    private Integer scheduledReinforcementId;
    private Integer reinforcementRequestId;
}
