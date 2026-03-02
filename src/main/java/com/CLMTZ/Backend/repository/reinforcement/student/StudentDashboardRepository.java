package com.CLMTZ.Backend.repository.reinforcement.student;

import com.CLMTZ.Backend.dto.reinforcement.student.StudentDashboardDTO;

public interface StudentDashboardRepository {
    StudentDashboardDTO getDashboard(Integer userId, Integer periodId);
}