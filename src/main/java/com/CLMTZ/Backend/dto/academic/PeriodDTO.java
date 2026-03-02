package com.CLMTZ.Backend.dto.academic;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PeriodDTO {
    private Integer periodId;
    private String period;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean state;

}
