package com.CLMTZ.Backend.dto.reinforcement;

import java.sql.Time;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReinforcementPerformedDTO {
    private Integer reinforcementPerformedId;
    private String observation;
    private Time duration;
    private Character state;
    private Integer scheduledReinforcementId;
}
