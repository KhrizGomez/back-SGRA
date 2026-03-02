package com.CLMTZ.Backend.dto.reinforcement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkAreaManagerDTO {
    private Integer workAreaManagerId;
    private Integer userId;
    private Integer areaAcademicId;
    private String plant;
    private Boolean state;
}
