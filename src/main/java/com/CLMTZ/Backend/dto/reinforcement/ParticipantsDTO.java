package com.CLMTZ.Backend.dto.reinforcement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParticipantsDTO {
    private Integer participantId;
    private Boolean stake;
    private Integer studentId;
    private Integer reinforcementRequestId;
}
