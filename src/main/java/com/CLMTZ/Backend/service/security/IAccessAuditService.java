package com.CLMTZ.Backend.service.security;

import java.util.List;

import com.CLMTZ.Backend.dto.security.Response.AccessAuditResponseDTO;

import jakarta.servlet.http.HttpServletRequest;

public interface IAccessAuditService {

    void createAccessAuditLogin(HttpServletRequest request, String attemptedUser, String action, Integer userId, String session);

    void createLogoutAuditLogin(HttpServletRequest request, Integer userId, String action, String session);

    List<AccessAuditResponseDTO> listAccessAudit();
}
