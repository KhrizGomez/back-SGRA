package com.CLMTZ.Backend.dto.security.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsersRolesRequestDTO {
    private Integer userRolesId;
    private Boolean state;
    private Integer roleId;
    private Integer userId;
}
