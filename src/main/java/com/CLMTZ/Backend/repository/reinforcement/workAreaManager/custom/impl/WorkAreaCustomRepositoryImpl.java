package com.CLMTZ.Backend.repository.reinforcement.workAreaManager.custom.impl;

import java.util.List;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.CLMTZ.Backend.config.DynamicDataSourceService;
import com.CLMTZ.Backend.dto.reinforcement.WorkAreaResponseDTO;
import com.CLMTZ.Backend.repository.reinforcement.workAreaManager.custom.IWorkAreaCustomRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class WorkAreaCustomRepositoryImpl  implements IWorkAreaCustomRepository{

    private final DynamicDataSourceService dynamicDataSourceService;

    @Override
    @Transactional(readOnly = true)
    public List<WorkAreaResponseDTO> listWorkAreas(Integer userId, Integer workAreaTypeId){
        String query = "select * from reforzamiento.fn_sl_refuerzo_areas_trabajo(:p_idusuario, :p_idtipoareatrabajo)";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("p_idusuario", userId)
                .addValue("p_idtipoareatrabajo", workAreaTypeId);

        return dynamicDataSourceService.getJdbcTemplate().query(query, params, new BeanPropertyRowMapper<>(WorkAreaResponseDTO.class));
    }
}
