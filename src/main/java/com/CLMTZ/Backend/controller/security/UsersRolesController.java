package com.CLMTZ.Backend.controller.security;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.CLMTZ.Backend.dto.security.Request.UsersRolesRequestDTO;
import com.CLMTZ.Backend.service.security.IUsersRolesService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/security/users-roles")
@RequiredArgsConstructor
public class UsersRolesController {
    
}
