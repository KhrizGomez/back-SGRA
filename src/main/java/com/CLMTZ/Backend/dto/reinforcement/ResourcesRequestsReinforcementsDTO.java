package com.CLMTZ.Backend.dto.reinforcement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResourcesRequestsReinforcementsDTO {
    private Integer resoucesRequestesReinforcementsId;
    private Integer reinforcementRequestId;
    private String urlFileRequestReinforcement;
}
