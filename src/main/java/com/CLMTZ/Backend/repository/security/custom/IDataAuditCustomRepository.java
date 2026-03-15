package com.CLMTZ.Backend.repository.security.custom;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.CLMTZ.Backend.dto.security.Response.DataAuditResponseDTO;

@Repository
public interface IDataAuditCustomRepository {

    List<DataAuditResponseDTO> listDataAudit(LocalDate dateFilter);
}
