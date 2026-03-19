package com.CLMTZ.Backend.dto.reinforcement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeacherAvailabilitySlotDTO {
    private Integer availabilityId;
    private Short dayOfWeek;
    private String dayName;
    private Integer timeSlotId;
    private String startTime;
    private String endTime;
}