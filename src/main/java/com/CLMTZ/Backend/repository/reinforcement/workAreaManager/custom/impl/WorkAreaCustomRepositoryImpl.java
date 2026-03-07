package com.CLMTZ.Backend.repository.reinforcement.workAreaManager.custom.impl;

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
import com.CLMTZ.Backend.dto.reinforcement.WorkAreaResponseDTO;
import com.CLMTZ.Backend.dto.reinforcement.workAreaManager.AssignWorkAreaReinforcementDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;
import com.CLMTZ.Backend.repository.reinforcement.workAreaManager.custom.IWorkAreaCustomRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class WorkAreaCustomRepositoryImpl  implements IWorkAreaCustomRepository{

    private final DynamicDataSourceService dynamicDataSourceService;

    @Override
    @Transactional(readOnly = true)
    public List<WorkAreaResponseDTO> listWorkAreas(Integer userId, Integer workAreaTypeId, Integer reinforcementId){
        String query = "select * from reforzamiento.fn_sl_refuerzo_areas_trabajo(:p_idusuario, :p_idtipoareatrabajo, :p_refuerzoprogramado)";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("p_idusuario", userId)
                .addValue("p_idtipoareatrabajo", workAreaTypeId)
                .addValue("p_refuerzoprogramado", reinforcementId);

        return dynamicDataSourceService.getJdbcTemplate().query(query, params, new BeanPropertyRowMapper<>(WorkAreaResponseDTO.class));
    }

    @Override
    @Transactional
    public SpResponseDTO AssignWorkAreaReinforcement (AssignWorkAreaReinforcementDTO assignWorkAreaReinforcement){
        
        String sql = "Call reforzamiento.sp_up_asignar_areatrabajo(?, ?, ?, ?)";

        JdbcTemplate jdbcTemplate = dynamicDataSourceService.getDefaultJdbcTemplate().getJdbcTemplate();

        return jdbcTemplate.execute(
            (Connection con) -> {
                CallableStatement cs = con.prepareCall(sql);

                cs.setInt(1, assignWorkAreaReinforcement.getPidrefuerzopresencial());
                cs.setInt(2, assignWorkAreaReinforcement.getPidareatrabajo());

                cs.registerOutParameter(3, Types.VARCHAR);
                cs.registerOutParameter(4, Types.BOOLEAN);

                return cs;
            },
            (CallableStatement cs) -> {

                cs.execute();

                String message = cs.getString(3);
                Boolean success = cs.getBoolean(4);

                return new SpResponseDTO(message,success);
            }
        );
    }
}
