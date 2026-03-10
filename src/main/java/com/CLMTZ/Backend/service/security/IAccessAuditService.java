package com.CLMTZ.Backend.service.security;

import java.util.List;

import com.CLMTZ.Backend.dto.security.Response.AccessAuditResponseDTO;
import com.CLMTZ.Backend.model.security.AccessAudit;

import jakarta.servlet.http.HttpServletRequest;

public interface IAccessAuditService {

    AccessAudit createAccessAuditLogin(HttpServletRequest request, String attemptedUser, String action, Integer userId, String session);

    void createLogoutAuditLogin(HttpServletRequest request, Integer userId, String action, String session);

    void forceLogout(String session);

    List<AccessAuditResponseDTO> listAccessAudit();
}
