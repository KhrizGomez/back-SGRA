package com.CLMTZ.Backend.controller.security;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.CLMTZ.Backend.config.CustomSessionRegistry;
import com.CLMTZ.Backend.dto.security.Response.AccessAuditResponseDTO;
import com.CLMTZ.Backend.service.security.IAccessAuditService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/security/audit")
@RequiredArgsConstructor
public class AuditController {

    private final IAccessAuditService accessAuditSer;
    private final CustomSessionRegistry customSessionReg;

    @GetMapping("/list-access-audit")
    public ResponseEntity<List<AccessAuditResponseDTO>> listAccessAudit(){
        List<AccessAuditResponseDTO>  accessAuditList = accessAuditSer.listAccessAudit();
        return ResponseEntity.ok(accessAuditList);
    }

    @DeleteMapping("/force-logout")
    public ResponseEntity<?> forceLogoutUser(@RequestParam String sessionId) {
        
        boolean success = customSessionReg.forceInvalidateSession(sessionId);
        
        if (success) {   
            accessAuditSer.forceLogout(sessionId);
            return ResponseEntity.ok(Map.of("message", "Sesión invalidada exitosamente"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "La sesión ya no está activa o no existe en el registro"));
        }
    }

}
