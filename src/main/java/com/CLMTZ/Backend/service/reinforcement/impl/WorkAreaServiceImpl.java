package com.CLMTZ.Backend.service.reinforcement.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.CLMTZ.Backend.dto.reinforcement.WorkAreaDTO;
import com.CLMTZ.Backend.model.reinforcement.WorkArea;
import com.CLMTZ.Backend.repository.reinforcement.IWorkAreaRepository;
import com.CLMTZ.Backend.repository.reinforcement.IWorkAreaTypesRepository;
import com.CLMTZ.Backend.service.reinforcement.IWorkAreaService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkAreaServiceImpl implements IWorkAreaService {

    private final IWorkAreaRepository workAreaRepo;
    private final IWorkAreaTypesRepository workAreaTypesRepository;

    @Override
    public List<WorkAreaDTO> findAll() { return workAreaRepo.findAll().stream().map(this::toDTO).collect(Collectors.toList()); }

    @Override
    public WorkAreaDTO findById(Integer id) { return workAreaRepo.findById(id).map(this::toDTO).orElseThrow(() -> new RuntimeException("WorkArea not found with id: " + id)); }

    @Override
    public WorkAreaDTO save(WorkAreaDTO dto) {
        WorkArea e = new WorkArea();
        e.setWorkArea(dto.getWorkArea()); e.setCapacity(dto.getCapacity()); e.setAvailability(dto.getAvailability());
        if (dto.getWorkAreaTypeId() != null) e.setWorkAreaTypeId(workAreaTypesRepository.findById(dto.getWorkAreaTypeId()).orElseThrow(() -> new RuntimeException("WorkAreaTypes not found")));
        return toDTO(workAreaRepo.save(e));
    }

    @Override
    public WorkAreaDTO update(Integer id, WorkAreaDTO dto) {
        WorkArea e = workAreaRepo.findById(id).orElseThrow(() -> new RuntimeException("WorkArea not found with id: " + id));
        e.setWorkArea(dto.getWorkArea()); e.setCapacity(dto.getCapacity()); e.setAvailability(dto.getAvailability());
        if (dto.getWorkAreaTypeId() != null) e.setWorkAreaTypeId(workAreaTypesRepository.findById(dto.getWorkAreaTypeId()).orElseThrow(() -> new RuntimeException("WorkAreaTypes not found")));
        return toDTO(workAreaRepo.save(e));
    }

    @Override
    public void deleteById(Integer id) { workAreaRepo.deleteById(id); }

    private WorkAreaDTO toDTO(WorkArea e) {
        WorkAreaDTO d = new WorkAreaDTO();
        d.setWorkAreaId(e.getWorkAreaId()); d.setWorkArea(e.getWorkArea()); d.setCapacity(e.getCapacity()); d.setAvailability(e.getAvailability());
        d.setWorkAreaTypeId(e.getWorkAreaTypeId() != null ? e.getWorkAreaTypeId().getWorkAreaTypeId() : null);
        return d;
    }

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
