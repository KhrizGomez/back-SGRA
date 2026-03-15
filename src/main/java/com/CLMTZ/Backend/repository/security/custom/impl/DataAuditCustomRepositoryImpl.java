package com.CLMTZ.Backend.repository.security.custom.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

import com.CLMTZ.Backend.config.DynamicDataSourceService;
import com.CLMTZ.Backend.dto.security.Response.DataAuditResponseDTO;
import com.CLMTZ.Backend.repository.security.custom.IDataAuditCustomRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class DataAuditCustomRepositoryImpl implements IDataAuditCustomRepository {

    private final DynamicDataSourceService dynamicDataSourceService;
    private final ObjectMapper objectMapper;

    @Override
    public List<DataAuditResponseDTO> listDataAudit(LocalDate dateFilter) {

        String query = "Select * from seguridad.fn_sl_auditoriadatos(:p_fechafiltro)";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("p_fechafiltro", dateFilter);

        return dynamicDataSourceService.getJdbcTemplate().query(query, params, (rs, rowNum) -> {
            DataAuditResponseDTO dto = new DataAuditResponseDTO();

            dto.setAusuario(rs.getString("ausuario"));
            dto.setAfechaacceso(rs.getObject("afechaacceso", LocalDateTime.class));
            dto.setAfechacierre(rs.getObject("afechacierre", LocalDateTime.class));
            dto.setAaccion(rs.getString("aaccion"));
            dto.setAtablaafectada(rs.getString("atablaafectada"));
            dto.setAidregistro(rs.getObject("aidregistro", Integer.class));
            dto.setAfechahoraaccion(rs.getObject("afechahoraaccion", LocalDateTime.class));

            String newDataJson = rs.getString("adatosnuevos");
            if (newDataJson != null) {
                try {
                    dto.setAdatosnuevos(objectMapper.readValue(newDataJson, new TypeReference<Map<String, Object>>() {}));
                } catch (Exception e) {
                    dto.setAdatosnuevos(null);
                }
            }

            String oldDataJson = rs.getString("adatosantiguos");
            if (oldDataJson != null) {
                try {
                    dto.setAdatosantiguos(objectMapper.readValue(oldDataJson, new TypeReference<Map<String, Object>>() {}));
                } catch (Exception e) {
                    dto.setAdatosantiguos(null);
                }
            }

            return dto;
        });
    }
}
