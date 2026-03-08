package com.CLMTZ.Backend.repository.reinforcement.workAreaManager.custom.impl;

import java.util.List;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.CLMTZ.Backend.config.DynamicDataSourceService;
import com.CLMTZ.Backend.dto.reinforcement.workAreaManager.ScheduleOccupancyDTO;
import com.CLMTZ.Backend.repository.reinforcement.workAreaManager.custom.IScheduleOccupancyCustomRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ScheduleOccupancyCustomRepositoryImpl implements IScheduleOccupancyCustomRepository {

    private final DynamicDataSourceService dynamicDataSourceService;

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleOccupancyDTO> listScheduleOccupancies(String filterText) {
        String query = "SELECT * FROM reforzamiento.fn_sl_ocupacion_horarios(:p_filtro_texto)";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("p_filtro_texto", filterText);

        return dynamicDataSourceService.getJdbcTemplate().query(query, params, new BeanPropertyRowMapper<>(ScheduleOccupancyDTO.class));
    }
}
