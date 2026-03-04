package com.CLMTZ.Backend.dto.reinforcement.teacher;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for accepting or rescheduling a reinforcement request (RF10, RF11).
 * estimatedDuration format: "HH:mm" (e.g. "01:30")
 * scheduledDate format: "YYYY-MM-DD"
 * startTime / endTime format: "HH:mm" (e.g. "07:00", "08:00")
 * The service resolves or creates the matching TimeSlot automatically.
 * For in-person sessions (modalidad = Presencial), workAreaId is required.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeacherScheduleRequestDTO {
    private String scheduledDate;
    private String startTime;           // replaces timeSlotId – "HH:mm"
    private String endTime;             // replaces timeSlotId – "HH:mm"
    private Integer modalityId;
    private String estimatedDuration;
    private String reason;
    private Integer workAreaTypeId; // required when modality is Presencial (1=Aula, 2=Laboratorio)
}
