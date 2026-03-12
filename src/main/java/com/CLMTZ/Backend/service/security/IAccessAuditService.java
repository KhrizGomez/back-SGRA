package com.CLMTZ.Backend.service.security;

import java.util.List;

import com.CLMTZ.Backend.dto.security.Response.AccessAuditResponseDTO;

import jakarta.servlet.http.HttpServletRequest;

public interface IAccessAuditService {

    void forceLogout(String session);

    List<AccessAuditResponseDTO> listAccessAudit();

    Integer auditAccess(HttpServletRequest request, Integer userId, String action, String session);
}
