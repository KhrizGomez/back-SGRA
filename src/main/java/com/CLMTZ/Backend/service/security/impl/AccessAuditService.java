package com.CLMTZ.Backend.service.security.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.CLMTZ.Backend.repository.security.jpa.IAccessAuditRepository;
import com.CLMTZ.Backend.service.security.IAccessAuditService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class AccessAuditService implements IAccessAuditService{

    private final IAccessAuditRepository accessAuditRepo;

    @Transactional
    public void createAccessAuditLogin(HttpServletRequest request, String attemptedUser, String action){

        String browser = request.getHeader("User-Agent");
        browser = extractBrowser(browser);

        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }

        accessAuditRepo.createAccessAudit(attemptedUser, ipAddress, browser, action);
    }

    private String extractBrowser(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "Desconocido";
        }

        String[] browsers = {"Edg", "OPR", "Chrome", "Firefox", "Safari"};

        for (String browser : browsers) {
            Pattern pattern = Pattern.compile(browser + "/[0-9.]+");
            Matcher matcher = pattern.matcher(userAgent);
            
            if (matcher.find()) {
                String result = matcher.group();
                
                if (result.startsWith("Edg/")) {
                    return result.replace("Edg", "Edge");
                }
                if (result.startsWith("OPR/")) {
                    return result.replace("OPR", "Opera");
                }
                
                return result;
            }
        }
        
        return "Otro";
    }
}
