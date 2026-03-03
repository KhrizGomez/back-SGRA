package com.CLMTZ.Backend.controller.security;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.CLMTZ.Backend.dto.security.Request.RoleManagementRequestDTO;
import com.CLMTZ.Backend.dto.security.Response.RoleListManagementResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.RoleListResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;
import com.CLMTZ.Backend.service.security.IRoleManagementService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/security/role-managements")
@RequiredArgsConstructor
public class RoleManagementController {

    private final IRoleManagementService roleManagementSer;

    @PostMapping("/create-role")
    public ResponseEntity<SpResponseDTO> createGRole(@RequestBody RoleManagementRequestDTO requestRole) {
        SpResponseDTO request = roleManagementSer.createRoleManagement(requestRole);
        return ResponseEntity.ok(request);
    }

    @PutMapping("/update-role")
    public ResponseEntity<SpResponseDTO> updateRoleManagement(@RequestBody RoleManagementRequestDTO requestRole) {
        SpResponseDTO request = roleManagementSer.updateRoleManagement(requestRole);
        return ResponseEntity.ok(request);
    }

    @GetMapping("/list-roles")
    public ResponseEntity<List<RoleListManagementResponseDTO>> listRoles(
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "state", required = false) Boolean state) {
        List<RoleListManagementResponseDTO> list = roleManagementSer.listRolesManagement(filter, state);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/list-rolesgroles-conexion")
    public ResponseEntity<List<RoleListResponseDTO>> listRoleManagementRole(){
        List<RoleListResponseDTO> list = roleManagementSer.listRoleManagementRole();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/list-roles-combo")
    public ResponseEntity<List<RoleManagementRequestDTO>> listRolesCombobox() {
        List<RoleManagementRequestDTO> list = roleManagementSer.listRoleNames();
        return ResponseEntity.ok(list);
    }
}
