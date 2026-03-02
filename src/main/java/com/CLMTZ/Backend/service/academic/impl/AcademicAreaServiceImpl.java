package com.CLMTZ.Backend.service.academic.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.CLMTZ.Backend.dto.academic.AcademicAreaDTO;
import com.CLMTZ.Backend.model.academic.AcademicArea;
import com.CLMTZ.Backend.model.general.Institution;
import com.CLMTZ.Backend.repository.academic.IAcademicAreaRepository;
import com.CLMTZ.Backend.repository.general.IInstitutionRepository;
import com.CLMTZ.Backend.service.academic.IAcademicAreaService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AcademicAreaServiceImpl implements IAcademicAreaService {

    private final IAcademicAreaRepository repository;
    private final IInstitutionRepository institutionRepository;

    @Override
    public List<AcademicAreaDTO> findAll() {
        return repository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public AcademicAreaDTO findById(Integer id) {
        return repository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("AcademicArea not found with id: " + id));
    }

    @Override
    public AcademicAreaDTO save(AcademicAreaDTO dto) {
        AcademicArea entity = toEntity(dto);
        return toDTO(repository.save(entity));
    }

    @Override
    public AcademicAreaDTO update(Integer id, AcademicAreaDTO dto) {
        AcademicArea entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("AcademicArea not found with id: " + id));
        entity.setNameArea(dto.getNameArea());
        entity.setAbbreviation(dto.getAbbreviation());
        entity.setLocation(dto.getLocation());
        entity.setState(dto.getState());
        if (dto.getInstitutionId() != null) {
            Institution institution = institutionRepository.findById(dto.getInstitutionId())
                    .orElseThrow(() -> new RuntimeException("Institution not found with id: " + dto.getInstitutionId()));
            entity.setInstitutionId(institution);
        }
        return toDTO(repository.save(entity));
    }

    @Override
    public void deleteById(Integer id) {
        repository.deleteById(id);
    }

    private AcademicAreaDTO toDTO(AcademicArea entity) {
        AcademicAreaDTO dto = new AcademicAreaDTO();
        dto.setAreaAcademicId(entity.getAreaAcademicId());
        dto.setNameArea(entity.getNameArea());
        dto.setAbbreviation(entity.getAbbreviation());
        dto.setLocation(entity.getLocation());
        dto.setState(entity.getState());
        dto.setInstitutionId(entity.getInstitutionId() != null ? entity.getInstitutionId().getInstitutionId() : null);
        return dto;
    }

    private AcademicArea toEntity(AcademicAreaDTO dto) {
        AcademicArea entity = new AcademicArea();
        entity.setNameArea(dto.getNameArea());
        entity.setAbbreviation(dto.getAbbreviation());
        entity.setLocation(dto.getLocation());
        entity.setState(dto.getState() != null ? dto.getState() : true);
        if (dto.getInstitutionId() != null) {
            Institution institution = institutionRepository.findById(dto.getInstitutionId())
                    .orElseThrow(() -> new RuntimeException("Institution not found with id: " + dto.getInstitutionId()));
            entity.setInstitutionId(institution);
        }
        return entity;
    }
}
