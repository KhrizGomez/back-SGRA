package com.CLMTZ.Backend.controller.security;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.CLMTZ.Backend.dto.security.Response.AccessAuditResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.DataAuditResponseDTO;
import com.CLMTZ.Backend.service.security.IAccessAuditService;
import com.CLMTZ.Backend.service.security.IDataAuditService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/security/audit")
@RequiredArgsConstructor
public class AuditController {

    private final IAccessAuditService accessAuditSer;
    private final IDataAuditService dataAuditSer;
    
    @GetMapping("/list-access-audit")
    public ResponseEntity<List<AccessAuditResponseDTO>> listAccessAudit(){
        List<AccessAuditResponseDTO>  accessAuditList = accessAuditSer.listAccessAudit();
        return ResponseEntity.ok(accessAuditList);
    }

    @DeleteMapping("/force-logout")
    public ResponseEntity<?> forceLogoutUser(@RequestParam Integer auditId) { 
        if(accessAuditSer.forceLogout(auditId)){
            return ResponseEntity.ok(Map.of("message", "Sesión invalidada exitosamente"));
        }
        else {
            return ResponseEntity.ok(Map.of("message", "Sesión invalidada fallida"));
        }
    }

    @GetMapping("/list-data-audit")
    public ResponseEntity<List<DataAuditResponseDTO>> listDataAudit(
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<DataAuditResponseDTO> dataAuditList = dataAuditSer.listDataAudit(date);
        return ResponseEntity.ok(dataAuditList);
    }

}
