package com.CLMTZ.Backend.repository.security.custom.impl;

import java.sql.Types;
import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.CLMTZ.Backend.config.DynamicDataSourceService;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.UserListManagementResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.UserRoleManagementResponseDTO;
import com.CLMTZ.Backend.repository.security.custom.IUserManagementCustomRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserManagementCustomRepositoryImpl implements IUserManagementCustomRepository{
    
    @PersistenceContext
    private EntityManager entityManager;

    private final DynamicDataSourceService dynamicDataSourceService;

    private NamedParameterJdbcTemplate getJdbcTemplate() {
        return dynamicDataSourceService.getJdbcTemplate();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserListManagementResponseDTO> listUsersManagement(String filterUser, LocalDate date, Boolean state) {
        String query = "SELECT * FROM seguridad.fn_sl_gusuarios(:p_filtro_usuario, :p_fecha, :p_estado)";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("p_filtro_usuario", filterUser)
                .addValue("p_fecha", date)
                .addValue("p_estado", state);
        
        return getJdbcTemplate().query(query, params, new BeanPropertyRowMapper<>(UserListManagementResponseDTO.class));
    }

    @Override
    @Transactional
    public SpResponseDTO createUserManagement(String user, String password, String roles) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("seguridad.sp_in_creargusuario");

        query.registerStoredProcedureParameter("p_gusuario", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_gcontrasena", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_roles", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_mensaje", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("p_exito", Boolean.class, ParameterMode.OUT);

        query.setParameter("p_gusuario", user);
        query.setParameter("p_gcontrasena", password);
        query.setParameter("p_roles", roles);

        query.execute();

        String message = (String) query.getOutputParameterValue("p_mensaje");
        Boolean success = (Boolean) query.getOutputParameterValue("p_exito");

        return new SpResponseDTO(message, success);
    }

    @Override
    public SpResponseDTO updateUserManagement(String jsonUserId){
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("seguridad.sp_up_gusuario");

        query.registerStoredProcedureParameter("p_json_usuario", String.class, ParameterMode.IN);

        query.registerStoredProcedureParameter("p_mensaje", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("p_exito", Boolean.class, ParameterMode.OUT);

        query.setParameter("p_json_usuario", jsonUserId);

        query.execute();

        String message = (String) query.getOutputParameterValue("p_mensaje");
        Boolean success = (Boolean) query.getOutputParameterValue("p_exito");

        return new SpResponseDTO(message, success);
    }

    @Override
    @Transactional(readOnly = true)
    public UserRoleManagementResponseDTO DataUserById(Integer idUser){
        String query = "Select * from seguridad.fn_sl_up_gusuariosroles(:p_iduserg)";
        
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("p_iduserg", idUser, Types.INTEGER);

        List<UserRoleManagementResponseDTO> resultados = getJdbcTemplate().query(query, params, (rs, rowNum) -> {
            UserRoleManagementResponseDTO dto = new UserRoleManagementResponseDTO();
            
            dto.setIdgu(rs.getInt(1));
            dto.setUsuariogu(rs.getString(2));
            dto.setContrasena(rs.getString(3));
            dto.setEstadogu(rs.getString(5));
            dto.setRolesasignadosgu(rs.getString(4));
            return dto;
        });

        return resultados.stream().findFirst().orElse(null);
    }
}
