package com.CLMTZ.Backend.service.security;

import java.util.List;

import com.CLMTZ.Backend.dto.security.Response.AccessAuditResponseDTO;

import jakarta.servlet.http.HttpServletRequest;

public interface IAccessAuditService {

    void createAccessAuditLogin(HttpServletRequest request, String attemptedUser, String action);

    void createLogoutAuditLogin(HttpServletRequest request, Integer userId, String action);

    List<AccessAuditResponseDTO> listAccessAudit();
}
