package com.CLMTZ.Backend.controller.security;

import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.CLMTZ.Backend.dto.security.Request.UserManagementRequestDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.UserListManagementResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.UserRoleManagementResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.UserRolesUpdateManagementResponseDTO;
import com.CLMTZ.Backend.service.security.IUserManagementService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/security/user-managements")
@CrossOrigin(origins = "*")
public class UserManagementController {

    private final IUserManagementService userManagementser;

    @GetMapping("/list-userG")
    public ResponseEntity<List<UserListManagementResponseDTO>> listUserG(
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "date", required = false) LocalDate date,
            @RequestParam(value = "state", required = false) Boolean state) {
        List<UserListManagementResponseDTO> requestList = userManagementser.listUserListManagement(filter, date, state);
        return ResponseEntity.ok(requestList);
    }

    @GetMapping("/list-userG-update")
    public ResponseEntity<UserRoleManagementResponseDTO> DataUserById(@RequestParam("idUser") Integer idUser) {
        UserRoleManagementResponseDTO request = userManagementser.DataUserById(idUser);
        return ResponseEntity.ok(request);
    }

    @PostMapping("/create-user")
    public ResponseEntity<SpResponseDTO> createUser(@RequestBody UserManagementRequestDTO requestUser) {

        SpResponseDTO request = userManagementser.createUserManagement(requestUser);

        return ResponseEntity.ok(request);
    }

    @PutMapping("/update-user")
    public ResponseEntity<SpResponseDTO> updateGUser(@RequestBody UserRolesUpdateManagementResponseDTO requestUser) {

        SpResponseDTO request = userManagementser.updateUserManagement(requestUser);

        return ResponseEntity.ok(request);
    }
}
