package com.CLMTZ.Backend.dto.reinforcement.teacher;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParticipantAttendanceDTO {
    private Integer attendanceId;
    private Integer participantId;
    private String studentName;
    private Boolean attended;
}
