package com.CLMTZ.Backend.service.reinforcement.student.impl;

import com.CLMTZ.Backend.config.UserContextHolder;
import com.CLMTZ.Backend.dto.reinforcement.student.*;
import com.CLMTZ.Backend.repository.reinforcement.student.StudentCatalogRepository;
import com.CLMTZ.Backend.service.reinforcement.student.StudentCatalogService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentCatalogServiceImpl implements StudentCatalogService {

    private final StudentCatalogRepository studentCatalogRepository;

    public StudentCatalogServiceImpl(StudentCatalogRepository studentCatalogRepository) {
        this.studentCatalogRepository = studentCatalogRepository;
    }

    @Override
    public List<SubjectItemDTO> getEnrolledSubjects() {
        Integer userId = UserContextHolder.getContext().getUserId();
        return studentCatalogRepository.listEnrolledSubjects(userId);
    }

    @Override
    public StudentSubjectTeacherDTO getTeacherForSubject(Integer subjectId) {
        Integer userId = UserContextHolder.getContext().getUserId();
        return studentCatalogRepository.getTeacherForStudentSubject(userId, subjectId);
    }

    @Override
    public List<SessionTypeItemDTO> getSessionTypes() {
        return studentCatalogRepository.listSessionTypes();
    }

    @Override
    public ActivePeriodDTO getActivePeriod() {
        return studentCatalogRepository.getActivePeriod();
    }

    @Override
    public List<ClassmateItemDTO> getClassmatesBySubject(Integer subjectId) {
        Integer userId = UserContextHolder.getContext().getUserId();
        return studentCatalogRepository.listClassmatesBySubject(subjectId, userId);
    }

    @Override
    public void addResourceUrl(Integer requestId, String fileUrl) {
        studentCatalogRepository.addResourceUrl(requestId, fileUrl);
    }
}