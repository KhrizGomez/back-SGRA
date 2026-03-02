package com.CLMTZ.Backend.dto.security.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ModuleManagementRequestDTO {
    private Integer roleGId;
    private String moduleG;
    private Boolean state;
}
