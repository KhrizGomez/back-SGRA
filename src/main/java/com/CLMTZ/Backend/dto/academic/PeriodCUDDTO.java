package com.CLMTZ.Backend.dto.academic;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PeriodCUDDTO {
    private Integer periodId;
    private String period;
    private String startDate;
    private String endDate;
    private Boolean state;
}
