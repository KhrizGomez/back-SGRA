package com.CLMTZ.Backend.dto.academic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AcademicAreaDTO {
    private Integer areaAcademicId;
    private String nameArea;
    private String abbreviation;
    private String location;
    private Boolean state;
    private Integer institutionId;
}
