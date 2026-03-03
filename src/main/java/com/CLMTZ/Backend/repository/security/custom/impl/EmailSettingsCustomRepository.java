package com.CLMTZ.Backend.repository.security.custom.impl;

import java.util.List;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.CLMTZ.Backend.config.DynamicDataSourceService;
import com.CLMTZ.Backend.dto.security.Request.EmailSettingsRequestDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;
import com.CLMTZ.Backend.repository.security.custom.IEmailSettingsCustomRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class EmailSettingsCustomRepository implements IEmailSettingsCustomRepository{

    @PersistenceContext
    private EntityManager entityManager;

    private final DynamicDataSourceService dynamicDataSourceService;

    @Override
    @Transactional(readOnly = true)
    public List<EmailSettingsRequestDTO> listEmailSettings(String filter, Boolean state){
        String query = "select * from seguridad.fn_sl_gconfiguracioncorreo(:p_filtro_texto, :p_estado)";

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("p_filtro_texto", filter).addValue("p_estado", state);

        return dynamicDataSourceService.getJdbcTemplate().query(query, params, new BeanPropertyRowMapper<>(EmailSettingsRequestDTO.class));
    }

    @Override
    @Transactional
    public SpResponseDTO createEmail(Integer userid, String email, String passwordApp){
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("seguridad.sp_in_configuracioncorreo");

        query.registerStoredProcedureParameter("p_idusuario", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_correo", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_contrasena", String.class, ParameterMode.IN);

        query.registerStoredProcedureParameter("p_mensaje", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("p_exito", Boolean.class, ParameterMode.OUT);

        query.setParameter("p_idusuario", userid);
        query.setParameter("p_correo", email);
        query.setParameter("p_contrasena", passwordApp);

        query.execute();

        String message = (String) query.getOutputParameterValue("p_mensaje");
        Boolean success = (Boolean) query.getOutputParameterValue("p_exito");

        return new SpResponseDTO(message, success);
    }
}
