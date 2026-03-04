package com.CLMTZ.Backend.controller.security;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.CLMTZ.Backend.dto.security.Response.AccessAuditResponseDTO;
import com.CLMTZ.Backend.service.security.IAccessAuditService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/security/audit")
@RequiredArgsConstructor
public class AuditController {

    private final IAccessAuditService accessAuditSer;

    @GetMapping("/list-access-audit")
    public ResponseEntity<List<AccessAuditResponseDTO>> listAccessAudit(){
        List<AccessAuditResponseDTO>  accessAuditList = accessAuditSer.listAccessAudit();
        return ResponseEntity.ok(accessAuditList);
    }

}
