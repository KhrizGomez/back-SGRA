package com.CLMTZ.Backend.service.academic.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.CLMTZ.Backend.dto.academic.RegistrationsDTO;
import com.CLMTZ.Backend.model.academic.Registrations;
import com.CLMTZ.Backend.repository.academic.IPeriodRepository;
import com.CLMTZ.Backend.repository.academic.IRegistrationsRepository;
import com.CLMTZ.Backend.repository.academic.IStudentsRepository;
import com.CLMTZ.Backend.service.academic.IRegistrationsService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegistrationsServiceImpl implements IRegistrationsService {

    private final IRegistrationsRepository repository;
    private final IPeriodRepository periodRepository;
    private final IStudentsRepository studentsRepository;

    @Override
    public List<RegistrationsDTO> findAll() {
        return repository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public RegistrationsDTO findById(Integer id) {
        return repository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Registration not found with id: " + id));
    }

    @Override
    public RegistrationsDTO save(RegistrationsDTO dto) {
        return toDTO(repository.save(toEntity(dto)));
    }

    @Override
    public RegistrationsDTO update(Integer id, RegistrationsDTO dto) {
        Registrations entity = repository.findById(id).orElseThrow(() -> new RuntimeException("Registration not found with id: " + id));
        entity.setDate(dto.getDate());
        entity.setStatus(dto.getStatus());
        if (dto.getPeriodId() != null) entity.setPeriodId(periodRepository.findById(dto.getPeriodId()).orElseThrow(() -> new RuntimeException("Period not found")));
        if (dto.getStudentId() != null) entity.setStudentId(studentsRepository.findById(dto.getStudentId()).orElseThrow(() -> new RuntimeException("Student not found")));
        return toDTO(repository.save(entity));
    }

    @Override
    public void deleteById(Integer id) { repository.deleteById(id); }

    private RegistrationsDTO toDTO(Registrations e) {
        RegistrationsDTO dto = new RegistrationsDTO();
        dto.setRegistrationId(e.getRegistrationId());
        dto.setDate(e.getDate());
        dto.setStatus(e.getStatus());
        dto.setPeriodId(e.getPeriodId() != null ? e.getPeriodId().getPeriodId() : null);
        dto.setStudentId(e.getStudentId() != null ? e.getStudentId().getStudentId() : null);
        return dto;
    }

    private Registrations toEntity(RegistrationsDTO dto) {
        Registrations entity = new Registrations();
        entity.setDate(dto.getDate());
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : true);
        if (dto.getPeriodId() != null) entity.setPeriodId(periodRepository.findById(dto.getPeriodId()).orElseThrow(() -> new RuntimeException("Period not found")));
        if (dto.getStudentId() != null) entity.setStudentId(studentsRepository.findById(dto.getStudentId()).orElseThrow(() -> new RuntimeException("Student not found")));
        return entity;
    }
}
