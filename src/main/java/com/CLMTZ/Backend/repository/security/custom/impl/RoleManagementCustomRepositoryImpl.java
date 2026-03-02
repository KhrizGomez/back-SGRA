package com.CLMTZ.Backend.repository.security.custom.impl;

import java.util.List;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.CLMTZ.Backend.config.DynamicDataSourceService;
import com.CLMTZ.Backend.dto.security.Request.RoleManagementRequestDTO;
import com.CLMTZ.Backend.dto.security.Response.FlatRoleMappingDTO;
import com.CLMTZ.Backend.dto.security.Response.RoleListManagementResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;
import com.CLMTZ.Backend.model.security.RoleManagement;
import com.CLMTZ.Backend.repository.security.IRoleManagementRepository;
import com.CLMTZ.Backend.repository.security.IRoleRepository;
import com.CLMTZ.Backend.repository.security.custom.IRoleManagementCustomRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RoleManagementCustomRepositoryImpl implements IRoleManagementCustomRepository{

    @PersistenceContext
    private EntityManager entityManager;

    private final IRoleRepository roleRepo;
    private final IRoleManagementRepository roleManagementRepo;

    private final DynamicDataSourceService dynamicDataSourceService;

    private NamedParameterJdbcTemplate getJdbcTemplate() {
        return dynamicDataSourceService.getJdbcTemplate();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleListManagementResponseDTO> listRolesManagement(String filter,Boolean state){
        String query = "Select * from seguridad.fn_sl_groles(:p_filtro_texto, :p_estado)";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("p_filtro_texto", filter != null ? filter : "")
                .addValue("p_estado", state);
        return getJdbcTemplate().query(query, params, new BeanPropertyRowMapper<>(RoleListManagementResponseDTO.class));
    }

    @Override
    @Transactional
    public SpResponseDTO createRoleManagement(String role, String description){
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("seguridad.sp_in_creargrol");

        query.registerStoredProcedureParameter("p_grol", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_descripcion", String.class, ParameterMode.IN);

        query.registerStoredProcedureParameter("p_mensaje", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("p_exito", Boolean.class, ParameterMode.OUT);

        query.setParameter("p_grol", role);
        query.setParameter("p_descripcion", description);

        query.execute();

        String message = (String) query.getOutputParameterValue("p_mensaje");
        Boolean success = (Boolean) query.getOutputParameterValue("p_exito");

        return new SpResponseDTO(message, success);
    }

    @Override
    @Transactional
    public SpResponseDTO updateRoleManagement(Integer roleId, String role, String description, Boolean state){
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("seguridad.sp_up_grol");

        query.registerStoredProcedureParameter("p_idgrol", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_grol", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_descripcion", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_estado", Boolean.class, ParameterMode.IN);

        query.registerStoredProcedureParameter("p_mensaje", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("p_exito", Boolean.class, ParameterMode.OUT);

        query.setParameter("p_idgrol", roleId);
        query.setParameter("p_grol", role);
        query.setParameter("p_descripcion", description);
        query.setParameter("p_estado", state);

        query.execute();

        String message = (String) query.getOutputParameterValue("p_mensaje");
        Boolean success = (Boolean) query.getOutputParameterValue("p_exito");

        return new SpResponseDTO(message, success);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlatRoleMappingDTO> listRoleManagementRole() {
        String query = "select * from seguridad.fn_sl_rolservidor_rolapp()";

        return getJdbcTemplate().query(query, new BeanPropertyRowMapper<>(FlatRoleMappingDTO.class));
    }
}
