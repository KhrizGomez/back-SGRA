package com.CLMTZ.Backend.dto.reinforcement.teacher;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for accepting or rescheduling a reinforcement request (RF10, RF11).
 * estimatedDuration format: "HH:mm" (e.g. "01:30")
 * scheduledDate format: "YYYY-MM-DD"
 * For in-person sessions (modalidad = Presencial), workAreaId is required.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeacherScheduleRequestDTO {
    private String scheduledDate;
    private Integer timeSlotId;
    private Integer modalityId;
    private String estimatedDuration;
    private String reason;
    private Integer workAreaId; // required when modality is presencial
}
