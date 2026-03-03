package com.CLMTZ.Backend.service.reinforcement.workAreaManager.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.CLMTZ.Backend.dto.reinforcement.WorkAreaDTO;
import com.CLMTZ.Backend.model.reinforcement.WorkArea;
import com.CLMTZ.Backend.repository.reinforcement.jpa.IWorkAreaRepository;
import com.CLMTZ.Backend.service.reinforcement.workAreaManager.IWorkAreaService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkAreaServiceImpl implements IWorkAreaService {

    private final IWorkAreaRepository workAreaRepo;
    
    @Override
    @Transactional(readOnly = true)
    public List<WorkAreaDTO> listAreasNames(Integer academicid){
        List<WorkArea> listNames = workAreaRepo.findByWorkAreaTypeId(academicid);

        return listNames.stream().map( Name -> {
            WorkAreaDTO dto = new WorkAreaDTO();
            dto.setWorkAreaId(Name.getWorkAreaId());
            dto.setWorkArea(Name.getWorkArea());
            dto.setCapacity(Name.getCapacity());
            return dto;
        }).toList();
    }
}
