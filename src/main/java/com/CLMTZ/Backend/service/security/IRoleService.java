package com.CLMTZ.Backend.service.security;

import java.util.List;

import com.CLMTZ.Backend.dto.security.Request.RoleRequestDTO;

public interface IRoleService {

    List<RoleRequestDTO> listRoleNames();
    
}
