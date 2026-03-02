package com.CLMTZ.Backend.repository.security.custom;

import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;

public interface IRoleManagementRoleCustomRepository {
    
    SpResponseDTO updateRoleGRoleAssignment(String jsonAssignment);
}
