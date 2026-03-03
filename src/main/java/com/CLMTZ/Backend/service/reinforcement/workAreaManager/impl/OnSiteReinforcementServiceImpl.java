package com.CLMTZ.Backend.service.reinforcement.workAreaManager.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.CLMTZ.Backend.dto.reinforcement.ListOfWorkAreaRequestsRequestDTO;
import com.CLMTZ.Backend.repository.reinforcement.workAreaManager.custom.IOnSiteReinforcementCustomRepository;
import com.CLMTZ.Backend.service.reinforcement.workAreaManager.IOnSiteReinforcementService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OnSiteReinforcementServiceImpl implements IOnSiteReinforcementService {

    private final IOnSiteReinforcementCustomRepository onSiteReinforcementCustomRepo;

        @Override
    @Transactional(readOnly =  true)
    public List<ListOfWorkAreaRequestsRequestDTO> listAreasRequests(Integer userId){
        try {
            List<ListOfWorkAreaRequestsRequestDTO> listAreasRequestsOf = onSiteReinforcementCustomRepo.listAreasRequests(userId);
            return listAreasRequestsOf;
        } catch (Exception e) {
            throw new RuntimeException("Error al listar las solicitudes para areas académicas: " +  e.getMessage()); 
        }
    }
}
