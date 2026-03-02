package com.CLMTZ.Backend.service.security.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

import com.CLMTZ.Backend.dto.security.Request.RoleManagementRoleRequestDTO;
import com.CLMTZ.Backend.dto.security.Request.UpdateAssignmentRolesGRolesRequestDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;
import com.CLMTZ.Backend.model.security.Role;
import com.CLMTZ.Backend.model.security.RoleManagement;
import com.CLMTZ.Backend.model.security.RoleManagementRole;
import com.CLMTZ.Backend.repository.security.IRoleManagementRepository;
import com.CLMTZ.Backend.repository.security.IRoleManagementRoleRepository;
import com.CLMTZ.Backend.repository.security.IRoleRepository;
import com.CLMTZ.Backend.repository.security.custom.IRoleManagementRoleCustomRepository;
import com.CLMTZ.Backend.service.security.IRoleManagementRoleService;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class RoleManagementRoleServiceImpl implements IRoleManagementRoleService {

    private final ObjectMapper objectMapper;
    private final IRoleManagementRoleCustomRepository roleManagementRoleCustomRepo;

    @Override
    public SpResponseDTO  updateRoleGRoleAssignment(List<UpdateAssignmentRolesGRolesRequestDTO> updateAssignmentRoles){
        try {
            String jsonAssignment = objectMapper.writeValueAsString(updateAssignmentRoles);
            return roleManagementRoleCustomRepo.updateRoleGRoleAssignment(jsonAssignment);
        } catch (Exception e) {
            throw new RuntimeException("Error al guardar las asignaciones de roles: " + e.getMessage());
        }
    }
}
