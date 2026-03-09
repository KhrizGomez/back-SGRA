package com.CLMTZ.Backend.repository.security.custom.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.CLMTZ.Backend.config.DynamicDataSourceService;
import com.CLMTZ.Backend.dto.security.Response.AccessAuditResponseDTO;
import com.CLMTZ.Backend.repository.security.custom.IAccessAuditCustomRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AccessAuditCustomRepositoryImpl implements IAccessAuditCustomRepository{

    private final DynamicDataSourceService dynamicDataSourceService;

    public List<AccessAuditResponseDTO> listAccessAudit() {

        String query = "select * from seguridad.fn_sl_auditoriaacceso()";

        return dynamicDataSourceService.getJdbcTemplate().query(query, (rs, rowNum) -> {
            AccessAuditResponseDTO dto = new AccessAuditResponseDTO();

            dto.setAidauditoriaacceso(rs.getInt(1));
            dto.setAusuario(rs.getString(2));
            dto.setAdireccionip(rs.getString(3));
            dto.setAnavegador(rs.getString(4));
            dto.setAfechaacceso(rs.getObject(5, LocalDateTime.class));
            dto.setAaccion(rs.getString(6));
            dto.setAso(rs.getString(7));
            dto.setAsesion(rs.getString(8));

            return dto;
        });
    }
}
