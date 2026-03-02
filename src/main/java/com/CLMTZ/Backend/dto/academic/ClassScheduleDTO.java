package com.CLMTZ.Backend.dto.academic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClassScheduleDTO {
    private Integer idClassSchedule;
    private Short day;
    private Boolean active;
    private Integer timeSlotId;
    private Integer assignedClassId;
    private Integer periodId;
}
