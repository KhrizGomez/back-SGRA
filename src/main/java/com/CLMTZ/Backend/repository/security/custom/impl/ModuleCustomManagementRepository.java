package com.CLMTZ.Backend.repository.security.custom.impl;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;
import java.util.List;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.CLMTZ.Backend.config.DynamicDataSourceService;
import com.CLMTZ.Backend.dto.security.Request.MasterDataManagementRequestDTO;
import com.CLMTZ.Backend.dto.security.Request.MasterManagementRequestDTO;
import com.CLMTZ.Backend.dto.security.Response.MasterDataListManagementResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.MasterTableListManagementResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.ModuleListManagementResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;
import com.CLMTZ.Backend.repository.security.custom.IModuleCustomManagementRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ModuleCustomManagementRepository implements IModuleCustomManagementRepository{

    private final DynamicDataSourceService dynamicDataSourceService;

    @Override
    @Transactional(readOnly = true)
    public List<ModuleListManagementResponseDTO> listModuleManagements(String grole){
        String query = "Select * from seguridad.fn_sl_privilegios_tablas_roles(:rol)";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("rol", grole);
        
        return dynamicDataSourceService.getJdbcTemplate().query(query, params, new BeanPropertyRowMapper<>(ModuleListManagementResponseDTO.class));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MasterTableListManagementResponseDTO> listMasterTables(){
        String query = "Select * from seguridad.fn_sl_tablas_maestras()";
        
        return dynamicDataSourceService.getJdbcTemplate().query(query, new BeanPropertyRowMapper<>(MasterTableListManagementResponseDTO.class));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MasterDataListManagementResponseDTO> listDataMasterTables(String schemaTable, String filtro){
        String query = "Select * from seguridad.fn_sl_datos_tablas_maestras(:p_esquematabla, :p_filtro)";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("p_esquematabla", schemaTable)
                .addValue("p_filtro", filtro);

        return dynamicDataSourceService.getJdbcTemplate().query(query, params, new BeanPropertyRowMapper<>(MasterDataListManagementResponseDTO.class));
    }

    @Override
    @Transactional
    public SpResponseDTO updateRolePermissions(String jsonPermissions){

        String sql = "CALL seguridad.sp_in_up_roles_permisos(?, ?, ?)";

        JdbcTemplate jdbcTemplate = dynamicDataSourceService.getJdbcTemplate().getJdbcTemplate();

        return jdbcTemplate.execute(
            (Connection con) -> {
                CallableStatement cs = con.prepareCall(sql);

                cs.setString(1, jsonPermissions);

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

    @Override
    @Transactional
    public SpResponseDTO masterTablesManagement(MasterManagementRequestDTO masterTables){

        String sql = "CALL seguridad.sp_in_tablas_maestras(?, ?, ?, ?)";

        JdbcTemplate jdbcTemplate = dynamicDataSourceService.getJdbcTemplate().getJdbcTemplate();

        return jdbcTemplate.execute(
            (Connection con) -> {
                CallableStatement cs = con.prepareCall(sql);

                cs.setString(1, masterTables.getEsquematabla());
                cs.setString(2, masterTables.getNombre());

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
    @Transactional
    public SpResponseDTO masterDataUpdateManagement(MasterDataManagementRequestDTO dataUpdate){

        String sql = "CALL seguridad.sp_up_tablas_maestras(?, ?, ?, ?, ?, ?)";

        JdbcTemplate jdbcTemplate = dynamicDataSourceService.getJdbcTemplate().getJdbcTemplate();

        return jdbcTemplate.execute(
            (Connection con) -> {
                CallableStatement cs = con.prepareCall(sql);

                cs.setString(1, dataUpdate.getEsquematabla());
                cs.setInt(2, dataUpdate.getId());
                cs.setString(3, dataUpdate.getNombre());
                cs.setBoolean(4, dataUpdate.getEstado());

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
}
