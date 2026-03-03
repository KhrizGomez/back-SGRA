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
import com.CLMTZ.Backend.dto.security.Request.EmailSettingsRequestDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;
import com.CLMTZ.Backend.repository.security.custom.IEmailSettingsCustomRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class EmailSettingsCustomRepository implements IEmailSettingsCustomRepository{

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

        String sql = "CALL seguridad.sp_in_configuracioncorreo(?, ?, ?, ?, ?)";

        JdbcTemplate jdbcTemplate = dynamicDataSourceService.getJdbcTemplate().getJdbcTemplate();

        return jdbcTemplate.execute(
            (Connection con) -> {
                CallableStatement cs = con.prepareCall(sql);

                cs.setInt(1, userid);
                cs.setString(2, email);
                cs.setString(3, passwordApp);

                cs.registerOutParameter(4, Types.VARCHAR);
                cs.registerOutParameter(5, Types.BOOLEAN);

                return cs;
            },
            (CallableStatement cs) -> {

                cs.execute();

                String message = cs.getString(4);
                Boolean success = cs.getBoolean(5);

                return new SpResponseDTO(message, success);
            }
        );
    }
}
