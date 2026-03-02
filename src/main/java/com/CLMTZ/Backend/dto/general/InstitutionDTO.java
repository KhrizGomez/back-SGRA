package com.CLMTZ.Backend.dto.general;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InstitutionDTO {
    private Integer institutionId;
    private String nameInstitution;
    private Boolean state;
}
