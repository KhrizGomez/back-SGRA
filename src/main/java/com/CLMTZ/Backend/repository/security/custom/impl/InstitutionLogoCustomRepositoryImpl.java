package com.CLMTZ.Backend.repository.security.custom.impl;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;
import java.util.List;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.CLMTZ.Backend.config.DynamicDataSourceService;
import com.CLMTZ.Backend.dto.general.InstitutionLogoResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;
import com.CLMTZ.Backend.repository.security.custom.IInstitutionLogoCustomRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class InstitutionLogoCustomRepositoryImpl implements IInstitutionLogoCustomRepository{

    private final DynamicDataSourceService dynamicDataSourceService;

    @Override
    public List<InstitutionLogoResponseDTO> listInstitutionLogo() {

        String query = "Select * from seguridad.fn_sl_logoinstitucion()";

        return dynamicDataSourceService.getJdbcTemplate().query(query, new BeanPropertyRowMapper<>(InstitutionLogoResponseDTO.class));
    }

    @Override
    public SpResponseDTO assignLogoInstitution (String jsonLogoInstitution){
        String sql = "Call seguridad.sp_in_logoinstitucion(?, ?, ?)";

        JdbcTemplate jdbcTemplate = dynamicDataSourceService.getJdbcTemplate().getJdbcTemplate();

        return jdbcTemplate.execute(
            (Connection con) -> {
                CallableStatement cs = con.prepareCall(sql);

                cs.setString(1, jsonLogoInstitution);

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
    public SpResponseDTO updateLogoInstitution (String jsonLogoInstitution){
        String sql = "Call seguridad.sp_up_logoinstitucion(?, ?, ?)";

        JdbcTemplate jdbcTemplate = dynamicDataSourceService.getJdbcTemplate().getJdbcTemplate();

        return jdbcTemplate.execute(
            (Connection con) -> {
                CallableStatement cs = con.prepareCall(sql);

                cs.setString(1, jsonLogoInstitution);

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
