package com.CLMTZ.Backend.controller.security;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.CLMTZ.Backend.dto.security.Request.RoleManagementRoleRequestDTO;
import com.CLMTZ.Backend.dto.security.Request.UpdateAssignmentRolesGRolesRequestDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;
import com.CLMTZ.Backend.service.security.IRoleManagementRoleService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/security/role-management-role")
@RequiredArgsConstructor
public class RoleManagementRoleController {

    private final IRoleManagementRoleService roleManagementRoleSer;

    @PutMapping("/update-assignment")
    public ResponseEntity<SpResponseDTO> updateRoleGRoleAssignment(@RequestBody List<UpdateAssignmentRolesGRolesRequestDTO> updateAssignmentRoles){
        SpResponseDTO responseDTO = roleManagementRoleSer.updateRoleGRoleAssignment(updateAssignmentRoles);
        return ResponseEntity.ok(responseDTO);
    }
}
