package com.CLMTZ.Backend.service.reinforcement.student;

import com.CLMTZ.Backend.dto.reinforcement.student.StudentCancelRequestResponseDTO;

public interface StudentRequestActionService {
    StudentCancelRequestResponseDTO cancelRequest(Integer userId, Integer requestId);
}