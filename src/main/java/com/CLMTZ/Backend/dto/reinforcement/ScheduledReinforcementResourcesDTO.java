package com.CLMTZ.Backend.dto.reinforcement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScheduledReinforcementResourcesDTO {
    private Integer scheduledReinforcementResourcesId;
    private Integer scheduledReinforcementId;
    private String urlFileScheduledReinforcement;
}
