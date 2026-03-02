package com.CLMTZ.Backend.service.reinforcement.student;

import com.CLMTZ.Backend.dto.reinforcement.student.StudentDashboardDTO;

public interface StudentDashboardService {
    StudentDashboardDTO getDashboard(Integer userId, Integer periodId);
}