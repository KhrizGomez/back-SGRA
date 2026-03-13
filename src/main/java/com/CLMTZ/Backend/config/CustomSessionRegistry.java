package com.CLMTZ.Backend.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.CLMTZ.Backend.dto.security.session.UserContext;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomSessionRegistry implements HttpSessionListener{

    private final Map<String, HttpSession> activeSessions = new ConcurrentHashMap<>();
    
    private final UserConnectionPool connectionPool;

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        HttpSession session = se.getSession();
        activeSessions.put(session.getId(), session);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        HttpSession session = se.getSession();
        
        try {
            UserContext ctx = (UserContext) session.getAttribute("CTX");
            if (ctx != null && ctx.getDbUser() != null) {
                connectionPool.evict(ctx.getDbUser());
            }
        } catch (Exception e) {}

        activeSessions.remove(session.getId());
    }

    public boolean forceInvalidateSession(String sessionId) {
        HttpSession session = activeSessions.get(sessionId);
        if (session != null) {
            try {
                session.invalidate();
                return true;
            } catch (IllegalStateException e) {
                activeSessions.remove(sessionId);
            }
        }
        return false;
    }
}
