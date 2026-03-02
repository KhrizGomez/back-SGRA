package com.CLMTZ.Backend.dto.security.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleManagementRoleRequestDTO {
    private Integer roleManagementRoleId;
    private Integer roleManagementId;
    private Integer roleId;
    private Boolean state;
}
