package com.CLMTZ.Backend.service.academic.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.CLMTZ.Backend.dto.academic.ModalityDTO;
import com.CLMTZ.Backend.model.academic.Modality;
import com.CLMTZ.Backend.repository.academic.IModalityRepository;
import com.CLMTZ.Backend.service.academic.IModalityService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ModalityServiceImpl implements IModalityService {

    private final IModalityRepository repository;

    @Override
    public List<ModalityDTO> findAll() {
        return repository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public ModalityDTO findById(Integer id) {
        return repository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Modality not found with id: " + id));
    }

    @Override
    public ModalityDTO save(ModalityDTO dto) {
        Modality entity = new Modality();
        entity.setModality(dto.getModality());
        entity.setState(dto.getState() != null ? dto.getState() : true);
        return toDTO(repository.save(entity));
    }

    @Override
    public ModalityDTO update(Integer id, ModalityDTO dto) {
        Modality entity = repository.findById(id).orElseThrow(() -> new RuntimeException("Modality not found with id: " + id));
        entity.setModality(dto.getModality());
        entity.setState(dto.getState());
        return toDTO(repository.save(entity));
    }

    @Override
    public void deleteById(Integer id) { repository.deleteById(id); }

    private ModalityDTO toDTO(Modality e) {
        ModalityDTO dto = new ModalityDTO();
        dto.setIdModality(e.getIdModality());
        dto.setModality(e.getModality());
        dto.setState(e.getState());
        return dto;
    }
}
