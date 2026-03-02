package com.CLMTZ.Backend.service.academic.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.CLMTZ.Backend.dto.academic.SubjectDTO;
import com.CLMTZ.Backend.dto.academic.SubjectLoadDTO;
import com.CLMTZ.Backend.model.academic.Subject;
import com.CLMTZ.Backend.repository.academic.ISubjectRepository;
import com.CLMTZ.Backend.service.academic.ISubjectService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements ISubjectService {

    private final ISubjectRepository repository;

    @Override
    public List<SubjectDTO> findAll() {
        return repository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public SubjectDTO findById(Integer id) {
        return repository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Subject not found with id: " + id));
    }

    @Override
    public SubjectDTO save(SubjectDTO dto) {
        Subject entity = new Subject();
        entity.setSubject(dto.getSubject());
        entity.setSemester(dto.getSemester());
        entity.setState(dto.getState() != null ? dto.getState() : true);
        return toDTO(repository.save(entity));
    }

    @Override
    public SubjectDTO update(Integer id, SubjectDTO dto) {
        Subject entity = repository.findById(id).orElseThrow(() -> new RuntimeException("Subject not found with id: " + id));
        entity.setSubject(dto.getSubject());
        entity.setSemester(dto.getSemester());
        entity.setState(dto.getState());
        return toDTO(repository.save(entity));
    }

    @Override
    public void deleteById(Integer id) { repository.deleteById(id); }

    private SubjectDTO toDTO(Subject e) {
        SubjectDTO dto = new SubjectDTO();
        dto.setIdSubject(e.getIdSubject());
        dto.setSubject(e.getSubject());
        dto.setSemester(e.getSemester());
        dto.setState(e.getState());
        return dto;
    }

    @Override
    public List<String> uploadSubjects(List<SubjectLoadDTO> subjectDTOs) {
        List<String> report = new java.util.ArrayList<>();
        for (SubjectLoadDTO dto : subjectDTOs) {
            try {
                Subject subject = repository.findAll().stream()
                    .filter(s -> s.getSubject().equalsIgnoreCase(dto.getNombreAsignatura()) && s.getSemester().equals(dto.getSemestre()))
                    .findFirst().orElse(null);
                if (subject == null) {
                    subject = new Subject();
                    subject.setSubject(dto.getNombreAsignatura());
                    subject.setSemester(dto.getSemestre());
                    subject.setState(true);
                    repository.save(subject);
                    report.add("Asignatura '" + dto.getNombreAsignatura() + "' creada");
                } else {
                    subject.setSemester(dto.getSemestre());
                    repository.save(subject);
                    report.add("Asignatura '" + dto.getNombreAsignatura() + "' actualizada");
                }
            } catch (Exception e) {
                report.add("Asignatura '" + dto.getNombreAsignatura() + "': ERROR (" + e.getMessage() + ")");
            }
        }
        return report;
    }
    
}
