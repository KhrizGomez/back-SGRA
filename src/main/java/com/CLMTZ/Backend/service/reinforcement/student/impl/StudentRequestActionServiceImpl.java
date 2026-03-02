package com.CLMTZ.Backend.service.reinforcement.student.impl;

import com.CLMTZ.Backend.dto.reinforcement.student.StudentCancelRequestResponseDTO;
import com.CLMTZ.Backend.repository.reinforcement.student.StudentRequestActionRepository;
import com.CLMTZ.Backend.service.reinforcement.student.StudentRequestActionService;
import org.springframework.stereotype.Service;

@Service
public class StudentRequestActionServiceImpl implements StudentRequestActionService {

    private final StudentRequestActionRepository studentRequestActionRepository;

    public StudentRequestActionServiceImpl(StudentRequestActionRepository studentRequestActionRepository) {
        this.studentRequestActionRepository = studentRequestActionRepository;
    }

    @Override
    public StudentCancelRequestResponseDTO cancelRequest(Integer userId, Integer requestId) {
        return studentRequestActionRepository.cancelRequest(userId, requestId);
    }
}