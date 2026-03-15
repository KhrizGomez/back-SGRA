package com.CLMTZ.Backend.service.security.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.CLMTZ.Backend.config.CustomSessionRegistry;
import com.CLMTZ.Backend.dto.security.Response.AccessAuditResponseDTO;
import com.CLMTZ.Backend.repository.security.custom.IAccessAuditCustomRepository;
import com.CLMTZ.Backend.service.security.IAccessAuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccessAuditServiceImpl implements IAccessAuditService{

    private static final Logger log = LoggerFactory.getLogger(AccessAuditServiceImpl.class);

    private final IAccessAuditCustomRepository accessAuditCustomRepo;
    private final CustomSessionRegistry customSessionReg;

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
    @Transactional
    public Boolean forceLogout(Integer auditAccessId){
        try {
            String session = accessAuditCustomRepo.sessionId(auditAccessId);
            boolean success = customSessionReg.forceInvalidateSession(session);
            if (success) {
                accessAuditCustomRepo.auditLogout(auditAccessId, "Cierre de sesion forzado");
                success = true;
            }
            System.out.println("auditAccessId: " + auditAccessId+ " session: " + session + " success: " + success);
            return success;
        } catch (Exception e) { 
            return false;
        }
    }

    @Override
    @Transactional
    public Integer auditAccess(HttpServletRequest request, Integer userId, String action, String session){
        try {
            String userAgent = request.getHeader("User-Agent");
            String browser = extractBrowser(userAgent);
            String deviceOs = extractOS(userAgent);

            String ipAddress = request.getHeader("X-Forwarded-For");
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddr();
            }

            return accessAuditCustomRepo.auditAccess(userId, ipAddress, browser, action, deviceOs, session);
        } catch (Exception e) { 
            return null;
        }
    }

    @Override
    @Transactional
    public void auditLogOut(Integer auditAccessId){
        try {
            accessAuditCustomRepo.auditLogout(auditAccessId, "Cierre de sesion");
        } catch (Exception e) {
            log.error("Error al registrar auditoría de cierre de sesión para auditAccessId={}: {}", auditAccessId, e.getMessage());
        }
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
