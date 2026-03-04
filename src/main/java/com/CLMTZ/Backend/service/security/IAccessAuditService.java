package com.CLMTZ.Backend.service.security;

import jakarta.servlet.http.HttpServletRequest;

public interface IAccessAuditService {

    void createAccessAuditLogin(HttpServletRequest request, String attemptedUser, String action);
}
