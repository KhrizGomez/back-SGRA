package com.CLMTZ.Backend.repository.security.custom;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.CLMTZ.Backend.dto.security.Response.AccessAuditResponseDTO;

@Repository
public interface IAccessAuditCustomRepository {
    
    List<AccessAuditResponseDTO> listAccessAudit();

    Integer auditAccess(Integer userId, String addressIp, String browser, String action, String so, String session);

    void auditLogout(Integer auditAccesId, String action);

    String sessionId(Integer auditAccessId);
}
