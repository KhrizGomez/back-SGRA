package com.CLMTZ.Backend.service.academic.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.CLMTZ.Backend.dto.academic.ClassDTO;
import com.CLMTZ.Backend.model.academic.Class;
import com.CLMTZ.Backend.repository.academic.*;
import com.CLMTZ.Backend.service.academic.IClassService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClassServiceImpl implements IClassService {

    private final IClassRepository repository;
    private final ITeachingRepository teachingRepository;
    private final ISubjectRepository subjectRepository;
    private final IPeriodRepository periodRepository;
    private final IParallelRepository parallelRepository;

    @Override
    public List<ClassDTO> findAll() {
        return repository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public ClassDTO findById(Integer id) {
        return repository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Class not found with id: " + id));
    }

    @Override
    public ClassDTO save(ClassDTO dto) {
        return toDTO(repository.save(toEntity(dto)));
    }

    @Override
    public ClassDTO update(Integer id, ClassDTO dto) {
        Class entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Class not found with id: " + id));
        entity.setActive(dto.getActive());
        if (dto.getTeacherId() != null) entity.setTeacherId(teachingRepository.findById(dto.getTeacherId()).orElseThrow(() -> new RuntimeException("Teaching not found")));
        if (dto.getSubjectId() != null) entity.setSubjectId(subjectRepository.findById(dto.getSubjectId()).orElseThrow(() -> new RuntimeException("Subject not found")));
        if (dto.getPeriodId() != null) entity.setPeriodId(periodRepository.findById(dto.getPeriodId()).orElseThrow(() -> new RuntimeException("Period not found")));
        if (dto.getParallelId() != null) entity.setParallelId(parallelRepository.findById(dto.getParallelId()).orElseThrow(() -> new RuntimeException("Parallel not found")));
        return toDTO(repository.save(entity));
    }

    @Override
    public void deleteById(Integer id) {
        repository.deleteById(id);
    }

    private ClassDTO toDTO(Class entity) {
        ClassDTO dto = new ClassDTO();
        dto.setIdClass(entity.getIdClass());
        dto.setActive(entity.getActive());
        dto.setTeacherId(entity.getTeacherId() != null ? entity.getTeacherId().getTeachingId() : null);
        dto.setSubjectId(entity.getSubjectId() != null ? entity.getSubjectId().getIdSubject() : null);
        dto.setPeriodId(entity.getPeriodId() != null ? entity.getPeriodId().getPeriodId() : null);
        dto.setParallelId(entity.getParallelId() != null ? entity.getParallelId().getParallelId() : null);
        return dto;
    }

    private Class toEntity(ClassDTO dto) {
        Class entity = new Class();
        entity.setActive(dto.getActive() != null ? dto.getActive() : true);
        if (dto.getTeacherId() != null) entity.setTeacherId(teachingRepository.findById(dto.getTeacherId()).orElseThrow(() -> new RuntimeException("Teaching not found")));
        if (dto.getSubjectId() != null) entity.setSubjectId(subjectRepository.findById(dto.getSubjectId()).orElseThrow(() -> new RuntimeException("Subject not found")));
        if (dto.getPeriodId() != null) entity.setPeriodId(periodRepository.findById(dto.getPeriodId()).orElseThrow(() -> new RuntimeException("Period not found")));
        if (dto.getParallelId() != null) entity.setParallelId(parallelRepository.findById(dto.getParallelId()).orElseThrow(() -> new RuntimeException("Parallel not found")));
        return entity;
    }
}
