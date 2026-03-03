package com.CLMTZ.Backend.service.reinforcement.workAreaManager;

import java.util.List;

import com.CLMTZ.Backend.dto.reinforcement.ListOfWorkAreaRequestsRequestDTO;

public interface IOnSiteReinforcementService {
    
    List<ListOfWorkAreaRequestsRequestDTO> listAreasRequests(Integer userId);
}
