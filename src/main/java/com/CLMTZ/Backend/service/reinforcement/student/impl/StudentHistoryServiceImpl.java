package com.CLMTZ.Backend.service.reinforcement.student.impl;

import com.CLMTZ.Backend.dto.reinforcement.student.StudentHistoryRequestsPageDTO;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentHistorySessionsPageDTO;
import com.CLMTZ.Backend.repository.reinforcement.student.StudentHistoryRepository;
import com.CLMTZ.Backend.service.reinforcement.student.StudentHistoryService;
import org.springframework.stereotype.Service;

@Service
public class StudentHistoryServiceImpl implements StudentHistoryService {

    private final StudentHistoryRepository studentHistoryRepository;

    public StudentHistoryServiceImpl(StudentHistoryRepository studentHistoryRepository) {
        this.studentHistoryRepository = studentHistoryRepository;
    }

    @Override
    public StudentHistoryRequestsPageDTO getRequestHistory(Integer userId, Integer periodId, Integer page, Integer size, Integer statusId) {
        return studentHistoryRepository.getRequestHistory(userId, periodId, page, size, statusId);
    }

    @Override
    public StudentHistorySessionsPageDTO getPreviousSessions(Integer userId, Integer page, Integer size, Boolean onlyAttended) {
        return studentHistoryRepository.getPreviousSessions(userId, page, size, onlyAttended);
    }
}