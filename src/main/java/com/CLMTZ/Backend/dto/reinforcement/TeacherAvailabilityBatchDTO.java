package com.CLMTZ.Backend.dto.reinforcement;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeacherAvailabilityBatchDTO {
    private Integer userId;
    private Integer periodId;
    private List<SlotDTO> slots;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SlotDTO {
        private Short dayOfWeek;
        private Integer timeSlotId;
    }
}
