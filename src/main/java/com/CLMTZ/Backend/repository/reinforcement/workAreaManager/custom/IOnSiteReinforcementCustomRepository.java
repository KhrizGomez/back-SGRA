package com.CLMTZ.Backend.repository.reinforcement.workAreaManager.custom;

import java.util.List;

import com.CLMTZ.Backend.dto.reinforcement.ListOfWorkAreaRequestsRequestDTO;

public interface IOnSiteReinforcementCustomRepository {

    List<ListOfWorkAreaRequestsRequestDTO> listAreasRequests(Integer userId);
    
}
