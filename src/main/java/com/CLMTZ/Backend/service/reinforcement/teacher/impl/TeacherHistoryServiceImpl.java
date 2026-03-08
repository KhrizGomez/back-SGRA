package com.CLMTZ.Backend.service.reinforcement.teacher.impl;

import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherSessionHistoryDetailDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherSessionHistoryPageDTO;
import com.CLMTZ.Backend.repository.reinforcement.teacher.TeacherHistoryRepository;
import com.CLMTZ.Backend.service.reinforcement.teacher.TeacherHistoryService;
import org.springframework.stereotype.Service;

@Service
public class TeacherHistoryServiceImpl implements TeacherHistoryService {

    private final TeacherHistoryRepository teacherHistoryRepository;

    public TeacherHistoryServiceImpl(TeacherHistoryRepository teacherHistoryRepository) {
        this.teacherHistoryRepository = teacherHistoryRepository;
    }

    @Override
    public TeacherSessionHistoryPageDTO getSessionHistory(Integer userId, Integer page, Integer size) {
        return teacherHistoryRepository.getSessionHistory(userId, page, size);
    }

    @Override
    public TeacherSessionHistoryDetailDTO getSessionHistoryDetail(Integer userId, Integer scheduledId) {
        return teacherHistoryRepository.getSessionHistoryDetail(userId, scheduledId);
    }
}
