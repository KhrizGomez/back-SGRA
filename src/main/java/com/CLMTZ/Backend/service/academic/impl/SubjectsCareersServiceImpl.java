package com.CLMTZ.Backend.service.academic.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.CLMTZ.Backend.dto.academic.SubjectsCareersDTO;
import com.CLMTZ.Backend.model.academic.SubjectsCareers;
import com.CLMTZ.Backend.repository.academic.ICareerRepository;
import com.CLMTZ.Backend.repository.academic.ISubjectRepository;
import com.CLMTZ.Backend.repository.academic.ISubjectsCareersRepository;
import com.CLMTZ.Backend.service.academic.ISubjectsCareersService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SubjectsCareersServiceImpl implements ISubjectsCareersService {

    private final ISubjectsCareersRepository repository;
    private final ISubjectRepository subjectRepository;
    private final ICareerRepository careerRepository;

    @Override
    public List<SubjectsCareersDTO> findAll() {
        return repository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public SubjectsCareersDTO findById(Integer id) {
        return repository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("SubjectsCareers not found with id: " + id));
    }

    @Override
    public SubjectsCareersDTO save(SubjectsCareersDTO dto) {
        return toDTO(repository.save(toEntity(dto)));
    }

    @Override
    public SubjectsCareersDTO update(Integer id, SubjectsCareersDTO dto) {
        SubjectsCareers entity = repository.findById(id).orElseThrow(() -> new RuntimeException("SubjectsCareers not found with id: " + id));
        entity.setState(dto.getState());
        if (dto.getSubjectId() != null) entity.setSubjectId(subjectRepository.findById(dto.getSubjectId()).orElseThrow(() -> new RuntimeException("Subject not found")));
        if (dto.getCareerId() != null) entity.setCareerId(careerRepository.findById(dto.getCareerId()).orElseThrow(() -> new RuntimeException("Career not found")));
        return toDTO(repository.save(entity));
    }

    @Override
    public void deleteById(Integer id) { repository.deleteById(id); }

    private SubjectsCareersDTO toDTO(SubjectsCareers e) {
        SubjectsCareersDTO dto = new SubjectsCareersDTO();
        dto.setSubjectCareerId(e.getSubjectCareerId());
        dto.setState(e.getState());
        dto.setSubjectId(e.getSubjectId() != null ? e.getSubjectId().getIdSubject() : null);
        dto.setCareerId(e.getCareerId() != null ? e.getCareerId().getCareerId() : null);
        return dto;
    }

    private SubjectsCareers toEntity(SubjectsCareersDTO dto) {
        SubjectsCareers entity = new SubjectsCareers();
        entity.setState(dto.getState() != null ? dto.getState() : true);
        if (dto.getSubjectId() != null) entity.setSubjectId(subjectRepository.findById(dto.getSubjectId()).orElseThrow(() -> new RuntimeException("Subject not found")));
        if (dto.getCareerId() != null) entity.setCareerId(careerRepository.findById(dto.getCareerId()).orElseThrow(() -> new RuntimeException("Career not found")));
        return entity;
    }
}
