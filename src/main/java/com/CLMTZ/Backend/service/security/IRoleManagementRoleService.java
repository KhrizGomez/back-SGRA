package com.CLMTZ.Backend.service.security;

import java.util.List;

import com.CLMTZ.Backend.dto.security.Request.RoleManagementRoleRequestDTO;
import com.CLMTZ.Backend.dto.security.Request.UpdateAssignmentRolesGRolesRequestDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;

public interface IRoleManagementRoleService {
    SpResponseDTO  updateRoleGRoleAssignment(List<UpdateAssignmentRolesGRolesRequestDTO> updateAssignmentRoles);
}
