package com.CLMTZ.Backend.service.reinforcement.student.impl;

import com.CLMTZ.Backend.dto.reinforcement.student.StudentDashboardDTO;
import com.CLMTZ.Backend.repository.reinforcement.student.StudentDashboardRepository;
import com.CLMTZ.Backend.service.reinforcement.student.StudentDashboardService;
import org.springframework.stereotype.Service;

@Service
public class StudentDashboardServiceImpl implements StudentDashboardService {

    private final StudentDashboardRepository studentDashboardRepository;

    public StudentDashboardServiceImpl(StudentDashboardRepository studentDashboardRepository) {
        this.studentDashboardRepository = studentDashboardRepository;
    }

    @Override
    public StudentDashboardDTO getDashboard(Integer userId, Integer periodId) {
        return studentDashboardRepository.getDashboard(userId, periodId);
    }
}