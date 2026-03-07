package com.CLMTZ.Backend.repository.academic.custom.impl;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.CLMTZ.Backend.config.DynamicDataSourceService;
import com.CLMTZ.Backend.dto.academic.PeriodCUDDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;
import com.CLMTZ.Backend.repository.academic.custom.IPeriodCustomRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PeriodCustomRepositoryImpl implements IPeriodCustomRepository{

    private final DynamicDataSourceService dynamicDataSourceSer;

    @Override
    @Transactional
    public SpResponseDTO createPeriod(PeriodCUDDTO periodCUD){

        String query = "Call academico.sp_in_periodoacademico(?, ?, ?, ?, ?)";
        
        JdbcTemplate jdbcTemplate = dynamicDataSourceSer.getJdbcTemplate().getJdbcTemplate();

        return jdbcTemplate.execute(
            (Connection con) -> {
                CallableStatement cs = con.prepareCall(query);

                cs.setString(1, periodCUD.getPeriod());
                cs.setString(2, periodCUD.getStartDate());
                cs.setString(3, periodCUD.getEndDate());

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
