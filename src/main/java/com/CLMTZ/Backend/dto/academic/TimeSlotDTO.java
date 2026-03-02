package com.CLMTZ.Backend.dto.academic;

import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TimeSlotDTO {
    private Integer timeSlotId;
    private LocalTime startTime;
    private LocalTime endTime;
    private Boolean state;
}
