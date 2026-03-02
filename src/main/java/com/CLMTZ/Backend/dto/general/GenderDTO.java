package com.CLMTZ.Backend.dto.general;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GenderDTO {
    private Integer genderId;
    private String genderName;
    private Boolean state;
}
