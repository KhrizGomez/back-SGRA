package com.CLMTZ.Backend.dto.reinforcement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReinforcementRequestStatusDTO {
    private Integer idReinforcementRequestStatus;
    private String nameState;
    private Boolean state;
}
