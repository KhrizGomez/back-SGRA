package com.CLMTZ.Backend.repository.security.custom;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.CLMTZ.Backend.dto.security.Response.AccessAuditResponseDTO;

@Repository
public interface IAccessAuditCustomRepository {
    List<AccessAuditResponseDTO> listAccessAudit();
}
