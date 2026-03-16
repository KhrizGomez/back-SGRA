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
import com.CLMTZ.Backend.dto.security.Request.EmailSettingsRequestDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;
import com.CLMTZ.Backend.repository.security.custom.IEmailSettingsCustomRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class EmailSettingsCustomRepositoryImpl implements IEmailSettingsCustomRepository{
    private final DynamicDataSourceService dynamicDataSourceService;

    @Override
    public List<EmailSettingsRequestDTO> listEmailSettings(String filter, Boolean state){
        String query = "select * from seguridad.fn_sl_gconfiguracioncorreo(:p_filtro_texto, :p_estado)";

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("p_filtro_texto", filter).addValue("p_estado", state);

        return dynamicDataSourceService.getJdbcTemplate().query(query, params, new BeanPropertyRowMapper<>(EmailSettingsRequestDTO.class));
    }

    @Override
    public SpResponseDTO createEmail(Integer userid, String email, String passwordApp, String servidorSmtp, Integer puertoSmtp, Boolean ssl, String nombreRemitente){

        String sql = "Call seguridad.sp_in_configuracioncorreo(?, ?, ?, ?, ?, ?, ?, ?, ?)";

        JdbcTemplate jdbcTemplate = dynamicDataSourceService.getJdbcTemplate().getJdbcTemplate();

        return jdbcTemplate.execute(
            (Connection con) -> {
                CallableStatement cs = con.prepareCall(sql);

                cs.setInt(1, userid);
                cs.setString(2, email);
                cs.setString(3, passwordApp);
                cs.setString(4, servidorSmtp);
                cs.setInt(5, puertoSmtp);
                cs.setBoolean(6, ssl);
                cs.setString(7, nombreRemitente);

                cs.registerOutParameter(8, Types.VARCHAR);
                cs.registerOutParameter(9, Types.BOOLEAN);

                return cs;
            },
            (CallableStatement cs) -> {

                cs.execute();

                String message = cs.getString(8);
                Boolean success = cs.getBoolean(9);

                return new SpResponseDTO(message, success);
            }
        );
    }

    @Override
    public SpResponseDTO updateEmail(Integer idConfiguracionCorreo, Integer userid, String email, String passwordApp, String servidorSmtp, Integer puertoSmtp, Boolean ssl, String nombreRemitente, Boolean estado) {

        String sql = "Call seguridad.sp_up_configuracioncorreo(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        JdbcTemplate jdbcTemplate = dynamicDataSourceService.getJdbcTemplate().getJdbcTemplate();

        return jdbcTemplate.execute(
            (Connection con) -> {
                CallableStatement cs = con.prepareCall(sql);

                cs.setInt(1, idConfiguracionCorreo);
                cs.setInt(2, userid);
                cs.setString(3, email);
                cs.setString(4, passwordApp);
                cs.setString(5, servidorSmtp);
                cs.setInt(6, puertoSmtp);
                cs.setBoolean(7, ssl);
                cs.setString(8, nombreRemitente);
                cs.setBoolean(9, estado);

                cs.registerOutParameter(10, Types.VARCHAR);
                cs.registerOutParameter(11, Types.BOOLEAN);

                return cs;
            },
            (CallableStatement cs) -> {

                cs.execute();

                String message = cs.getString(10);
                Boolean success = cs.getBoolean(11);

                return new SpResponseDTO(message, success);
            }
        );
    }

    @Override
    public EmailSettingsRequestDTO getEmailById(Integer idConfiguracionCorreo) {
        String query = "Select * from seguridad.fn_sl_up_configuracioncorreo(:p_idconfiguracioncorreo)";

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("p_idconfiguracioncorreo", idConfiguracionCorreo);

        return dynamicDataSourceService.getJdbcTemplate().queryForObject(query, params, EmailSettingsRequestDTO.class);
    }
}
