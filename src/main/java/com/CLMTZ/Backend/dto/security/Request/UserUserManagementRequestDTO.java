package com.CLMTZ.Backend.dto.security.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserUserManagementRequestDTO {
    private Integer userUserGId;
    private Integer userId;
    private Integer userManagementId;
    private Boolean state;
}
