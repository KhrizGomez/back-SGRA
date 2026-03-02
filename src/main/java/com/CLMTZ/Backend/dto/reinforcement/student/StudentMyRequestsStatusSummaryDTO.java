package com.CLMTZ.Backend.dto.reinforcement.student;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentMyRequestsStatusSummaryDTO {
    private Integer statusId;
    private String statusJson;
    private Long total;
}