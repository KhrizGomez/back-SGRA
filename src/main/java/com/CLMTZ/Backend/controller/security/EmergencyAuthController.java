package com.CLMTZ.Backend.controller.security;

import com.CLMTZ.Backend.dto.security.session.UserContext;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/emergency")
public class EmergencyAuthController {

    @Value("${sgra.emergency.password}")
    private String emergencyPassword;

    private static final String SESSION_CTX_KEY = "CTX";

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> emergencyLogin(
            @RequestBody Map<String, String> body,
            HttpSession session) {

        String password = body.get("password");
        if (password == null || !emergencyPassword.equals(password)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Contraseña de emergencia incorrecta"));
        }

        UserContext ctx = new UserContext();
        ctx.setUserId(-1);
        ctx.setUsername("emergency_admin");
        ctx.setFirstName("Administrador");
        ctx.setLastName("Emergencia");
        ctx.setEmail("");
        ctx.setRoles(List.of("admin"));
        ctx.setServerSynced(false);
        ctx.setAccountState('A');

        session.setAttribute(SESSION_CTX_KEY, ctx);

        return ResponseEntity.ok(Map.of("success", true));
    }
}