package com.CLMTZ.Backend.repository.security.custom.impl;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;
import java.util.List;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

import com.CLMTZ.Backend.config.DynamicDataSourceService;
import com.CLMTZ.Backend.dto.security.Response.FlatRoleMappingDTO;
import com.CLMTZ.Backend.dto.security.Response.RoleListManagementResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;
import com.CLMTZ.Backend.repository.security.custom.IRoleManagementCustomRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RoleManagementCustomRepositoryImpl implements IRoleManagementCustomRepository{

    private final DynamicDataSourceService dynamicDataSourceService;

    @Override
    public List<RoleListManagementResponseDTO> listRolesManagement(String filter,Boolean state){
        String query = "Select * from seguridad.fn_sl_groles(:p_filtro_texto, :p_estado)";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("p_filtro_texto", filter != null ? filter : "")
                .addValue("p_estado", state);
        return dynamicDataSourceService.getJdbcTemplate().query(query, params, new BeanPropertyRowMapper<>(RoleListManagementResponseDTO.class));
    }

    @Override
    public SpResponseDTO createRoleManagement(String role, String description){

        String sql = "CALL seguridad.sp_in_creargrol(?, ?, ?, ?)";

        JdbcTemplate jdbcTemplate = dynamicDataSourceService.getJdbcTemplate().getJdbcTemplate();

        return jdbcTemplate.execute(
            (Connection con) -> {
                CallableStatement cs = con.prepareCall(sql);

                cs.setString(1, role);
                cs.setString(2, description);

                cs.registerOutParameter(3, Types.VARCHAR);
                cs.registerOutParameter(4, Types.BOOLEAN);

                return cs;
            },
            (CallableStatement cs) -> {

                cs.execute();

                String message = cs.getString(3);
                Boolean success = cs.getBoolean(4);

                return new SpResponseDTO(message, success);
            }
        );
    }

    @Override
    public SpResponseDTO updateRoleManagement(Integer roleId, String role, String description, Boolean state){

        String sql = "CALL seguridad.sp_up_grol(?, ?, ?, ?, ?, ?)";

        JdbcTemplate jdbcTemplate = dynamicDataSourceService.getJdbcTemplate().getJdbcTemplate();

        return jdbcTemplate.execute(
            (Connection con) -> {
                CallableStatement cs = con.prepareCall(sql);

                cs.setInt(1, roleId);
                cs.setString(2, role);
                cs.setString(3, description);
                cs.setBoolean(4, state);

                cs.registerOutParameter(5, Types.VARCHAR);
                cs.registerOutParameter(6, Types.BOOLEAN);

                return cs;
            },
            (CallableStatement cs) -> {

                cs.execute();

                String message = cs.getString(5);
                Boolean success = cs.getBoolean(6);

                return new SpResponseDTO(message, success);
            }
        );
    }

    @Override
    public List<FlatRoleMappingDTO> listRoleManagementRole() {
        String query = "select * from seguridad.fn_sl_rolservidor_rolapp()";

        return dynamicDataSourceService.getJdbcTemplate().query(query, new BeanPropertyRowMapper<>(FlatRoleMappingDTO.class));
    }
}
