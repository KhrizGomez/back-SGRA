package com.CLMTZ.Backend.service.academic.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.CLMTZ.Backend.dto.academic.ParallelDTO;
import com.CLMTZ.Backend.model.academic.Parallel;
import com.CLMTZ.Backend.repository.academic.IParallelRepository;
import com.CLMTZ.Backend.service.academic.IParallelService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ParallelServiceImpl implements IParallelService {

    private final IParallelRepository repository;

    @Override
    public List<ParallelDTO> findAll() {
        return repository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public ParallelDTO findById(Integer id) {
        return repository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Parallel not found with id: " + id));
    }

    @Override
    public ParallelDTO save(ParallelDTO dto) {
        Parallel entity = new Parallel();
        entity.setSection(dto.getSection());
        entity.setActive(dto.getActive() != null ? dto.getActive() : true);
        return toDTO(repository.save(entity));
    }

    @Override
    public ParallelDTO update(Integer id, ParallelDTO dto) {
        Parallel entity = repository.findById(id).orElseThrow(() -> new RuntimeException("Parallel not found with id: " + id));
        entity.setSection(dto.getSection());
        entity.setActive(dto.getActive());
        return toDTO(repository.save(entity));
    }

    @Override
    public void deleteById(Integer id) { repository.deleteById(id); }

    private ParallelDTO toDTO(Parallel e) {
        ParallelDTO dto = new ParallelDTO();
        dto.setParallelId(e.getParallelId());
        dto.setSection(e.getSection());
        dto.setActive(e.getActive());
        return dto;
    }
}
