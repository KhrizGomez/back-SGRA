package com.CLMTZ.Backend.service.security.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.CLMTZ.Backend.dto.security.Response.AccessAuditResponseDTO;
import com.CLMTZ.Backend.model.general.User;
import com.CLMTZ.Backend.model.security.AccessAudit;
import com.CLMTZ.Backend.repository.general.IUserRepository;
import com.CLMTZ.Backend.repository.security.custom.IAccessAuditCustomRepository;
import com.CLMTZ.Backend.repository.security.jpa.IAccessAuditRepository;
import com.CLMTZ.Backend.service.security.IAccessAuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccessAuditService implements IAccessAuditService{

    private final IAccessAuditRepository accessAuditRepo;
    private final IAccessAuditCustomRepository accessAuditCustomRepo;
    private final IUserRepository userRepo;

    @Override
    public void createAccessAuditLogin(HttpServletRequest request, String attemptedUser, String action, Integer userId, String session){

        try {
            String userAgent = request.getHeader("User-Agent");
            String browser = extractBrowser(userAgent);
            String deviceOs = extractOS(userAgent);

            User user = userRepo.findById(userId).orElse(null);

            String ipAddress = request.getHeader("X-Forwarded-For");
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddr();
            }

            LocalDateTime dateTime = LocalDateTime.now();
            AccessAudit accessAudit = new AccessAudit(null, user, ipAddress, browser, dateTime, deviceOs, session, action);

            accessAuditRepo.save(accessAudit);
        } catch (Exception e) { }
    }

    @Override
    public void createLogoutAuditLogin(HttpServletRequest request, Integer userId, String action, String session){

        try {
            String userAgent = request.getHeader("User-Agent");
            String browser = extractBrowser(userAgent);
            String deviceOs = extractOS(userAgent);

            User user = userRepo.findById(userId).orElse(null);

            String ipAddress = request.getHeader("X-Forwarded-For");
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddr();
            }

            LocalDateTime dateTime = LocalDateTime.now();
            AccessAudit accessAudit = new AccessAudit(null, user, ipAddress, browser, dateTime, deviceOs, session, action);

            accessAuditRepo.save(accessAudit);
        } catch (Exception e) { }

    }

    @Override
    @Transactional(readOnly = true)
    public List<AccessAuditResponseDTO> listAccessAudit(){

        try {
            return accessAuditCustomRepo.listAccessAudit();
        } catch (Exception e) {
            throw new RuntimeException("Error al listar la auditoria de sesiones: "+ e.getMessage());
        }

    }

    @Override
    public void forceLogout(String session){
        try {
            AccessAudit accessAudit = accessAuditRepo.findBySession(session);
            accessAudit.setAccessDate(LocalDateTime.now());
            accessAudit.setAction("Cierre sesión forzado");
            accessAuditRepo.save(accessAudit);
        } catch (Exception e) { }
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

    private String extractOS(String userAgent) {
    if (userAgent == null || userAgent.isEmpty()) return "Desconocido";
    if (userAgent.toLowerCase().contains("windows")) return "Windows";
    if (userAgent.toLowerCase().contains("mac")) return "MacOS";
    if (userAgent.toLowerCase().contains("android")) return "Android";
    if (userAgent.toLowerCase().contains("iphone") || userAgent.toLowerCase().contains("ipad")) return "iOS";
    if (userAgent.toLowerCase().contains("linux")) return "Linux";
    return "Otro";
}
}
