package com.CLMTZ.Backend.controller.security;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.CLMTZ.Backend.dto.security.Request.RoleRequestDTO;
import com.CLMTZ.Backend.service.security.IRoleService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/security/roles")
@RequiredArgsConstructor
public class RoleController {

    private final IRoleService roleSer;

    @GetMapping("/list-roles-aplication")
    public ResponseEntity<List<RoleRequestDTO>> listRolesAplicationC(){
        List<RoleRequestDTO> requestListRoles = roleSer.listRoleNames();
        return ResponseEntity.ok(requestListRoles);
    }
}
