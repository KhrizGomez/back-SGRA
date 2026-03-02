package com.CLMTZ.Backend.repository.security.custom;

import java.util.List;

import com.CLMTZ.Backend.dto.security.Response.FlatRoleMappingDTO;
import com.CLMTZ.Backend.dto.security.Response.RoleListManagementResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;

public interface IRoleManagementCustomRepository {
    List<RoleListManagementResponseDTO> listRolesManagement(String filter, Boolean state);

    SpResponseDTO createRoleManagement(String role, String description);

    SpResponseDTO updateRoleManagement(Integer roleId, String role, String description, Boolean state);

    List<FlatRoleMappingDTO> listRoleManagementRole();
}
