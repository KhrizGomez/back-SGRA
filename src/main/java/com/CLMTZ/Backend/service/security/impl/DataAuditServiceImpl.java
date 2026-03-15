package com.CLMTZ.Backend.service.security.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.CLMTZ.Backend.dto.security.Response.DataAuditResponseDTO;
import com.CLMTZ.Backend.repository.security.custom.IDataAuditCustomRepository;
import com.CLMTZ.Backend.service.security.IDataAuditService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DataAuditServiceImpl implements IDataAuditService {

    private final IDataAuditCustomRepository dataAuditCustomRepo;

    @Override
    @Transactional(readOnly = true)
    public List<DataAuditResponseDTO> listDataAudit(LocalDate dateFilter) {
        try {
            return dataAuditCustomRepo.listDataAudit(dateFilter);
        } catch (Exception e) {
            throw new RuntimeException("Error al listar la auditoría de datos: " + e.getMessage());
        }
    }
}
