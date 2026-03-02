package com.CLMTZ.Backend.service.security;

import com.CLMTZ.Backend.dto.security.Request.LoginRequestDTO;
import com.CLMTZ.Backend.dto.security.Response.LoginResponseDTO;
import com.CLMTZ.Backend.dto.security.session.UserContext;
import jakarta.servlet.http.HttpSession;

public interface IAuthService {
    LoginResponseDTO login(LoginRequestDTO request, HttpSession session);
    LoginResponseDTO getCurrentUser(HttpSession session);
    void logout(HttpSession session);
    UserContext getUserContext(HttpSession session);
}

