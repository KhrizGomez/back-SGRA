package com.CLMTZ.Backend.service.academic.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.CLMTZ.Backend.dto.academic.StudentsDTO;
import com.CLMTZ.Backend.model.academic.Students;
import com.CLMTZ.Backend.repository.academic.ICareerRepository;
import com.CLMTZ.Backend.repository.academic.IStudentsRepository;
import com.CLMTZ.Backend.repository.general.IUserRepository;
import com.CLMTZ.Backend.service.academic.IStudentsService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentsServiceImpl implements IStudentsService {

    private final IStudentsRepository repository;
    private final IUserRepository userRepository;
    private final ICareerRepository careerRepository;

    @Override
    public List<StudentsDTO> findAll() {
        return repository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public StudentsDTO findById(Integer id) {
        return repository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
    }

    @Override
    public StudentsDTO save(StudentsDTO dto) {
        return toDTO(repository.save(toEntity(dto)));
    }

    @Override
    public StudentsDTO update(Integer id, StudentsDTO dto) {
        Students entity = repository.findById(id).orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
        entity.setState(dto.getState());
        if (dto.getUserId() != null) entity.setUserId(userRepository.findById(dto.getUserId()).orElseThrow(() -> new RuntimeException("User not found")));
        if (dto.getCareerId() != null) entity.setCareerId(careerRepository.findById(dto.getCareerId()).orElseThrow(() -> new RuntimeException("Career not found")));
        return toDTO(repository.save(entity));
    }

    @Override
    public void deleteById(Integer id) { repository.deleteById(id); }

    private StudentsDTO toDTO(Students e) {
        StudentsDTO dto = new StudentsDTO();
        dto.setStudentId(e.getStudentId());
        dto.setState(e.getState());
        dto.setUserId(e.getUserId() != null ? e.getUserId().getUserId() : null);
        dto.setCareerId(e.getCareerId() != null ? e.getCareerId().getCareerId() : null);
        return dto;
    }

    private Students toEntity(StudentsDTO dto) {
        Students entity = new Students();
        entity.setState(dto.getState() != null ? dto.getState() : true);
        if (dto.getUserId() != null) entity.setUserId(userRepository.findById(dto.getUserId()).orElseThrow(() -> new RuntimeException("User not found")));
        if (dto.getCareerId() != null) entity.setCareerId(careerRepository.findById(dto.getCareerId()).orElseThrow(() -> new RuntimeException("Career not found")));
        return entity;
    }
}
