package com.CLMTZ.Backend.controller.security;

import com.CLMTZ.Backend.dto.security.Request.ForgotPasswordRequestDTO;
import com.CLMTZ.Backend.dto.security.Request.ResetPasswordRequestDTO;
import com.CLMTZ.Backend.dto.security.Request.VerifyCodeRequestDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;
import com.CLMTZ.Backend.service.security.IPasswordRecoveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordRecoveryController {

    private final IPasswordRecoveryService passwordRecoveryService;

    /**
     * Paso 1: El usuario ingresa su correo y se le envía un código de 6 dígitos.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequestDTO request) {
        SpResponseDTO result = passwordRecoveryService.requestRecoveryCode(request.getEmail());
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

    /**
     * Paso 2: El usuario ingresa el código de verificación recibido por correo.
     */
    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody VerifyCodeRequestDTO request) {
        SpResponseDTO result = passwordRecoveryService.verifyRecoveryCode(request.getEmail(), request.getCode());
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

    /**
     * Paso 3: El usuario establece su nueva contraseña.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequestDTO request) {
        SpResponseDTO result = passwordRecoveryService.resetPassword(
                request.getEmail(),
                request.getCode(),
                request.getNewPassword(),
                request.getConfirmPassword()
        );
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

