package com.CLMTZ.Backend.repository.security.custom.impl;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.CLMTZ.Backend.config.DynamicDataSourceService;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;
import com.CLMTZ.Backend.repository.security.custom.IRoleManagementRoleCustomRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RoleManagementRoleCustomRepository implements IRoleManagementRoleCustomRepository{
    
    private final DynamicDataSourceService dynamicDataSourceService;

    @Override
    @Transactional
    public SpResponseDTO updateRoleGRoleAssignment(String jsonAssignment){

        String sql = "CALL seguridad.sp_in_up_asignacionroles(?, ?, ?)";

        JdbcTemplate jdbcTemplate = dynamicDataSourceService.getJdbcTemplate().getJdbcTemplate();

        return jdbcTemplate.execute(
            (Connection con) -> {
                CallableStatement cs = con.prepareCall(sql);

                cs.setString(1, jsonAssignment);

                cs.registerOutParameter(2, Types.VARCHAR);
                cs.registerOutParameter(3, Types.BOOLEAN);

                return cs;
            },
            (CallableStatement cs) -> {

                cs.execute();

                String message = cs.getString(2);
                Boolean success = cs.getBoolean(3);

                return new SpResponseDTO(message, success);
            }
        );
    }
}
