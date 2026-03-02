package com.CLMTZ.Backend.config;

import com.CLMTZ.Backend.dto.security.session.UserContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@Order(1)
public class SessionAuthFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(SessionAuthFilter.class);
    private static final String SESSION_CTX_KEY = "CTX";

    // Rutas públicas que no requieren autenticación
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/api/auth/login",
            "/api/auth/me"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        // Permitir OPTIONS (preflight CORS)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            chain.doFilter(request, response);
            return;
        }

        // Verificar si es una ruta pública
        if (isPublicPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        // Verificar si hay sesión activa
        HttpSession session = httpRequest.getSession(false);
        if (session == null) {
            log.debug("No hay sesión activa para la ruta: {}", path);
            sendUnauthorized(httpResponse, "No hay sesión activa");
            return;
        }

        UserContext ctx = (UserContext) session.getAttribute(SESSION_CTX_KEY);
        if (ctx == null) {
            log.debug("No hay contexto de usuario en sesión para la ruta: {}", path);
            sendUnauthorized(httpResponse, "No hay sesión activa");
            return;
        }

        // Poblar UserContextHolder para acceso desde cualquier parte del código
        try {
            UserContextHolder.setContext(ctx);
            log.debug("UserContext establecido para usuario: {} (dbUser: {})", ctx.getUsername(), ctx.getDbUser());

            // Sesión válida, continuar
            chain.doFilter(request, response);
        } finally {
            // IMPORTANTE: Limpiar siempre al final del request para evitar memory leaks
            UserContextHolder.clear();
        }
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}

