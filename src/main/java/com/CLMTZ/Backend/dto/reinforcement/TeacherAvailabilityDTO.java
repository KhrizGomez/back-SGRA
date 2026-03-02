package com.CLMTZ.Backend.dto.reinforcement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeacherAvailabilityDTO {
    private Integer teacherAvailabilityId;
    private Short dayOfWeek;
    private Boolean state;
    private Integer teacherId;
    private Integer timeSlotId;
    private Integer periodId;
}
