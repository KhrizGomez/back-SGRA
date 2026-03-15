package com.CLMTZ.Backend.service.security;

import java.time.LocalDate;
import java.util.List;

import com.CLMTZ.Backend.dto.security.Response.DataAuditResponseDTO;

public interface IDataAuditService {

    List<DataAuditResponseDTO> listDataAudit(LocalDate dateFilter);
}
