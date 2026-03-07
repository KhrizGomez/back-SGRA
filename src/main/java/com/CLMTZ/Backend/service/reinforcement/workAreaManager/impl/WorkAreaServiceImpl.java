package com.CLMTZ.Backend.service.reinforcement.workAreaManager.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.CLMTZ.Backend.dto.reinforcement.WorkAreaResponseDTO;
import com.CLMTZ.Backend.dto.reinforcement.workAreaManager.AssignWorkAreaReinforcementDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;
import com.CLMTZ.Backend.repository.reinforcement.workAreaManager.custom.IWorkAreaCustomRepository;
import com.CLMTZ.Backend.service.reinforcement.workAreaManager.IWorkAreaService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkAreaServiceImpl implements IWorkAreaService {

    private final IWorkAreaCustomRepository workAreaCustomRepo;
    
    @Override
    @Transactional(readOnly = true)
    public List<WorkAreaResponseDTO> listWorkAreas(Integer userId, Integer workAreaTypeId){
        try {
            return workAreaCustomRepo.listWorkAreas(userId, workAreaTypeId);
        } catch (Exception e) {
            throw new RuntimeException("Error al listar las areas de trabajo: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public SpResponseDTO AssignWorkAreaReinforcement (AssignWorkAreaReinforcementDTO assignWorkAreaReinforcement){
        try {
            return workAreaCustomRepo.AssignWorkAreaReinforcement(assignWorkAreaReinforcement);
        } catch (Exception e) {
            return new SpResponseDTO("Error al asignar un area académica", false);
        }
    }
}
