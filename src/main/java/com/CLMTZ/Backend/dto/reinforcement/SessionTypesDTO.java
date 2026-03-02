package com.CLMTZ.Backend.dto.reinforcement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SessionTypesDTO {
    private Integer sesionTypesId;
    private String sesionType;
    private Boolean state;
}
