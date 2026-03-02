package com.CLMTZ.Backend.dto.academic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CareerDTO {
    private Integer careerId;
    private String nameCareer;
    private Short semester;
    private Boolean state;
    private Integer academicAreaId;
    private Integer modalityId;
}
