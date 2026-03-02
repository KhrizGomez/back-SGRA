package com.CLMTZ.Backend.dto.reinforcement;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkAreaDTO {
    private Integer workAreaId;
    private String workArea;
    private Integer capacity;
    private Character availability;
    private Integer workAreaTypeId;
}
