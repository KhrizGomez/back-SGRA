package com.CLMTZ.Backend.dto.security.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRoleManagementRequestDTO {
    private Integer userRoleGId;
    private Boolean state;
    private Integer userManagementId;
    private Integer roleManagementId;
}
