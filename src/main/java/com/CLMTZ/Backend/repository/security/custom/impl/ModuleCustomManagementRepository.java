package com.CLMTZ.Backend.repository.security.custom.impl;

import java.util.List;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ModuleCustomManagementRepository implements IModuleCustomManagementRepository{
    @PersistenceContext
    private EntityManager entityManager;

    private final DynamicDataSourceService dynamicDataSourceService;

    private NamedParameterJdbcTemplate getJdbcTemplate() {
        return dynamicDataSourceService.getJdbcTemplate();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModuleListManagementResponseDTO> listModuleManagements(String grole){
        String query = "Select * from seguridad.fn_sl_privilegios_tablas_roles(:rol)";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("rol", grole);
        
        return getJdbcTemplate().query(query, params, new BeanPropertyRowMapper<>(ModuleListManagementResponseDTO.class));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MasterTableListManagementResponseDTO> listMasterTables(){
        String query = "Select * from seguridad.fn_sl_tablas_maestras()";
        
        return getJdbcTemplate().query(query, new BeanPropertyRowMapper<>(MasterTableListManagementResponseDTO.class));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MasterDataListManagementResponseDTO> listDataMasterTables(String schemaTable, String filtro){
        String query = "Select * from seguridad.fn_sl_datos_tablas_maestras(:p_esquematabla, :p_filtro)";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("p_esquematabla", schemaTable)
                .addValue("p_filtro", filtro);

        return getJdbcTemplate().query(query, params, new BeanPropertyRowMapper<>(MasterDataListManagementResponseDTO.class));
    }

    @Override
    @Transactional
    public SpResponseDTO updateRolePermissions(String jsonPermissions){
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("seguridad.sp_in_up_roles_permisos");

        query.registerStoredProcedureParameter("p_permisos", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_mensaje", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("p_exito", Boolean.class, ParameterMode.OUT);

        query.setParameter("p_permisos", jsonPermissions);

        query.execute();

        String message = (String) query.getOutputParameterValue("p_mensaje");
        Boolean success = (Boolean) query.getOutputParameterValue("p_exito");

        return new SpResponseDTO(message, success);
    }

    @Override
    @Transactional
    public SpResponseDTO masterTablesManagement(MasterManagementRequestDTO masterTables){
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("seguridad.sp_in_tablas_maestras");

        query.registerStoredProcedureParameter("p_esquematabla", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_valor", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_mensaje", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("p_exito", Boolean.class, ParameterMode.OUT);

        query.setParameter("p_esquematabla", masterTables.getEsquematabla());
        query.setParameter("p_valor", masterTables.getNombre());

        query.execute();

        String message = (String) query.getOutputParameterValue("p_mensaje");
        Boolean success = (Boolean) query.getOutputParameterValue("p_exito");

        return new SpResponseDTO(message, success);
    }

    @Override
    @Transactional
    public SpResponseDTO masterDataUpdateManagement(MasterDataManagementRequestDTO dataUpdate){
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("seguridad.sp_up_tablas_maestras");

        query.registerStoredProcedureParameter("p_esquematabla", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_id", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_valor", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_estado", Boolean.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_mensaje", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("p_exito", Boolean.class, ParameterMode.OUT);

        query.setParameter("p_esquematabla", dataUpdate.getEsquematabla());
        query.setParameter("p_id", dataUpdate.getId());
        query.setParameter("p_valor", dataUpdate.getNombre());
        query.setParameter("p_estado", dataUpdate.getEstado());

        query.execute();

        String message = (String) query.getOutputParameterValue("p_mensaje");
        Boolean success = (Boolean) query.getOutputParameterValue("p_exito");

        return new SpResponseDTO(message, success);
    }
}
