package com.CLMTZ.Backend.dto.security.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleManagementModuleRequestDTO {
    private Integer roleModuleGId;
    private Boolean state;
    private Integer roleManagementId;
    private Integer moduleManagementId;
}
