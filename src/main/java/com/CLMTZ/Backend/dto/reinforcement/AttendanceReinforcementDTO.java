package com.CLMTZ.Backend.dto.reinforcement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceReinforcementDTO {
    private Integer attendanceId;
    private Boolean attendance;
    private Integer reinforcementPerformedId;
    private Integer studentId;
}
