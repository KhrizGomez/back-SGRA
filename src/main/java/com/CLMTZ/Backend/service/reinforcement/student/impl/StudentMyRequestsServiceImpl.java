package com.CLMTZ.Backend.service.reinforcement.student.impl;

import com.CLMTZ.Backend.dto.reinforcement.student.StudentMyRequestsChipsDTO;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentMyRequestsPageDTO;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentMyRequestsStatusSummaryDTO;
import com.CLMTZ.Backend.repository.reinforcement.student.StudentMyRequestsRepository;
import com.CLMTZ.Backend.service.reinforcement.student.StudentMyRequestsService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentMyRequestsServiceImpl implements StudentMyRequestsService {

    private final StudentMyRequestsRepository studentMyRequestsRepository;

    public StudentMyRequestsServiceImpl(StudentMyRequestsRepository studentMyRequestsRepository) {
        this.studentMyRequestsRepository = studentMyRequestsRepository;
    }

    @Override
    public StudentMyRequestsPageDTO getMyRequests(Integer userId, Integer periodId, Integer statusId,
                                                   Integer sessionTypeId, Integer subjectId, String search,
                                                   Integer page, Integer size) {
        return studentMyRequestsRepository.getMyRequests(userId, periodId, statusId, sessionTypeId,
                subjectId, search, page, size);
    }

    @Override
    public StudentMyRequestsChipsDTO getMyRequestsChips(Integer userId, Integer periodId) {
        return studentMyRequestsRepository.getMyRequestsChips(userId, periodId);
    }

    @Override
    public List<StudentMyRequestsStatusSummaryDTO> getMyRequestsSummary(Integer userId, Integer periodId) {
        return studentMyRequestsRepository.getMyRequestsSummary(userId, periodId);
    }
}