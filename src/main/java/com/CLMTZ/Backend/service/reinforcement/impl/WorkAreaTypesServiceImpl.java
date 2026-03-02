package com.CLMTZ.Backend.service.reinforcement.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.CLMTZ.Backend.dto.reinforcement.WorkAreaTypesDTO;
import com.CLMTZ.Backend.model.reinforcement.WorkAreaTypes;
import com.CLMTZ.Backend.repository.reinforcement.IWorkAreaTypesRepository;
import com.CLMTZ.Backend.service.reinforcement.IWorkAreaTypesService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkAreaTypesServiceImpl implements IWorkAreaTypesService {

    private final IWorkAreaTypesRepository repository;

    @Override
    public List<WorkAreaTypesDTO> findAll() { return repository.findAll().stream().map(this::toDTO).collect(Collectors.toList()); }

    @Override
    public WorkAreaTypesDTO findById(Integer id) { return repository.findById(id).map(this::toDTO).orElseThrow(() -> new RuntimeException("WorkAreaTypes not found with id: " + id)); }

    @Override
    public WorkAreaTypesDTO save(WorkAreaTypesDTO dto) {
        WorkAreaTypes e = new WorkAreaTypes();
        e.setWorkAreaType(dto.getWorkAreaType()); e.setState(dto.getState() != null ? dto.getState() : true);
        return toDTO(repository.save(e));
    }

    @Override
    public WorkAreaTypesDTO update(Integer id, WorkAreaTypesDTO dto) {
        WorkAreaTypes e = repository.findById(id).orElseThrow(() -> new RuntimeException("WorkAreaTypes not found with id: " + id));
        e.setWorkAreaType(dto.getWorkAreaType()); e.setState(dto.getState());
        return toDTO(repository.save(e));
    }

    @Override
    public void deleteById(Integer id) { repository.deleteById(id); }

    private WorkAreaTypesDTO toDTO(WorkAreaTypes e) {
        WorkAreaTypesDTO d = new WorkAreaTypesDTO();
        d.setWorkAreaTypeId(e.getWorkAreaTypeId()); d.setWorkAreaType(e.getWorkAreaType()); d.setState(e.getState());
        return d;
    }
}
