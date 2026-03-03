package com.CLMTZ.Backend.repository.reinforcement.workAreaManager.custom.impl;

import java.util.List;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.CLMTZ.Backend.config.DynamicDataSourceService;
import com.CLMTZ.Backend.dto.reinforcement.ListOfWorkAreaRequestsRequestDTO;
import com.CLMTZ.Backend.repository.reinforcement.workAreaManager.custom.IOnSiteReinforcementCustomRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OnSiteReinforcementCustomRepositoryImpl implements IOnSiteReinforcementCustomRepository{

    private final DynamicDataSourceService dynamicDataSourceService;

    @Override
    @Transactional(readOnly = true)
    public List<ListOfWorkAreaRequestsRequestDTO> listAreasRequests(Integer userId){
        String query = "select * from reforzamiento.fn_sl_refuerzo_presencial_areatrabajo(:p_idareaacademica)";

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("p_idareaacademica", userId);

        return dynamicDataSourceService.getJdbcTemplate().query(query, params, new BeanPropertyRowMapper<>(ListOfWorkAreaRequestsRequestDTO.class)); 
    }
}
