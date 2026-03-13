package com.CLMTZ.Backend.controller.security;

import com.CLMTZ.Backend.dto.security.Request.ChangePasswordRequestDTO;
import com.CLMTZ.Backend.dto.security.Request.LoginRequestDTO;
import com.CLMTZ.Backend.dto.security.Request.VoluntaryChangePasswordRequestDTO;
import com.CLMTZ.Backend.dto.security.Response.LoginResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;
import com.CLMTZ.Backend.service.security.IAuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO request, HttpSession session, HttpServletRequest requestSer) {
        try {
            LoginResponseDTO response = authService.login(request, session,requestSer);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest requestSer) {
        HttpSession session = requestSer.getSession(false);
        if (session == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No hay sesion activa"));
        }
        LoginResponseDTO user = authService.getCurrentUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No hay sesion activa"));
        }
        return ResponseEntity.ok(user);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session, HttpServletRequest requestSer) {  
        authService.logout(session, requestSer);
        return ResponseEntity.ok(Map.of("message", "Sesion cerrada correctamente"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequestDTO request, HttpSession session) {
        SpResponseDTO result = authService.changePassword(request, session);
        if (Boolean.TRUE.equals(result.getSuccess())) {
            return ResponseEntity.ok(Map.of(
                    "message", result.getMessage(),
                    "success", true
            ));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", result.getMessage(),
                            "success", false
                    ));
        }
    }

    @PostMapping("/voluntary-change-password")
    public ResponseEntity<?> voluntaryChangePassword(@RequestBody VoluntaryChangePasswordRequestDTO request, HttpSession session) {
        SpResponseDTO result = authService.voluntaryChangePassword(request, session);
        if (Boolean.TRUE.equals(result.getSuccess())) {
            return ResponseEntity.ok(Map.of(
                    "message", result.getMessage(),
                    "success", true
            ));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", result.getMessage(),
                            "success", false
                    ));
        }
    }
}
