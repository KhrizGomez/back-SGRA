package com.CLMTZ.Backend.repository.security.custom.impl;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

import com.CLMTZ.Backend.config.DynamicDataSourceService;
import com.CLMTZ.Backend.dto.security.Response.AccessAuditResponseDTO;
import com.CLMTZ.Backend.repository.security.custom.IAccessAuditCustomRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AccessAuditCustomRepositoryImpl implements IAccessAuditCustomRepository{

    private final DynamicDataSourceService dynamicDataSourceService;

    @Override
    public List<AccessAuditResponseDTO> listAccessAudit() {

        String query = "select * from seguridad.fn_sl_auditoriaacceso()";

        return dynamicDataSourceService.getJdbcTemplate().query(query, (rs, rowNum) -> {
            AccessAuditResponseDTO dto = new AccessAuditResponseDTO();

            dto.setAidauditoriaacceso(rs.getInt(1));
            dto.setAusuario(rs.getString(2));
            dto.setAdireccionip(rs.getString(3));
            dto.setAnavegador(rs.getString(4));
            dto.setAfechaacceso(rs.getObject(5, LocalDateTime.class));
            dto.setAfechacierre(rs.getObject(6, LocalDateTime.class));
            dto.setAaccion(rs.getString(7));
            dto.setAso(rs.getString(8));
            dto.setAsesion(rs.getString(9));

            return dto;
        });
    }

    @Override
    public Integer auditAccess(Integer userId, String addressIp, String browser, String action, String so, String session){

        String sql = "Call seguridad.sp_in_auditoriaacceso(?, ?, ?, ?, ?, ?, ?)";

        JdbcTemplate jdbcTemplate = dynamicDataSourceService.getJdbcTemplate().getJdbcTemplate();

        return jdbcTemplate.execute(
            (Connection con) -> {
                CallableStatement cs = con.prepareCall(sql);

                cs.setInt(1, userId);
                cs.setString(2, addressIp);
                cs.setString(3, browser);
                cs.setString(4, action);
                cs.setString(5, so);
                cs.setString(6, session);

                cs.registerOutParameter(7, Types.INTEGER);

                return cs;
            },
            (CallableStatement cs) -> {

                cs.execute();

                return cs.getInt(7);
            }
        );
        
    }

    @Override
    public void auditLogout(Integer auditAccesId, String action){
        String sql = "Call seguridad.sp_up_auditoriaacceso(?, ?)";

        JdbcTemplate jdbcTemplate = dynamicDataSourceService.getDefaultJdbcTemplate().getJdbcTemplate();

        jdbcTemplate.execute(
            (Connection con) -> {
                CallableStatement cs = con.prepareCall(sql);

                cs.setInt(1, auditAccesId);
                cs.setString(2, action);

                cs.execute();

                return null;
            });
    }

    @Override
    public String sessionId(Integer auditAccessId){

        String query = "Select seguridad.fn_vltext_sesion(:p_idauditoriaacceso)";

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("p_idauditoriaacceso", auditAccessId);

        return dynamicDataSourceService.getJdbcTemplate().queryForObject(query, params, String.class);
    }
}
