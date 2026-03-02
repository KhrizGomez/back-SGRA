package com.CLMTZ.Backend.dto.reinforcement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OnSiteReinforcementDTO {
    private Integer onSiteReinforcementId;
    private Boolean state;
    private Integer scheduledReinforcementId;
    private Integer workAreaId;
}
