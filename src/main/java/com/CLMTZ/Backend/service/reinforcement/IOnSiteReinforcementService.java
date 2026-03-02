package com.CLMTZ.Backend.service.reinforcement;

import java.util.List;

import com.CLMTZ.Backend.dto.reinforcement.ListOfWorkAreaRequestsRequestDTO;
import com.CLMTZ.Backend.dto.reinforcement.OnSiteReinforcementDTO;

public interface IOnSiteReinforcementService {
    List<OnSiteReinforcementDTO> findAll();
    OnSiteReinforcementDTO findById(Integer id);
    OnSiteReinforcementDTO save(OnSiteReinforcementDTO dto);
    OnSiteReinforcementDTO update(Integer id, OnSiteReinforcementDTO dto);
    void deleteById(Integer id);

    List<ListOfWorkAreaRequestsRequestDTO> listAreasRequests(Integer userId);
}
