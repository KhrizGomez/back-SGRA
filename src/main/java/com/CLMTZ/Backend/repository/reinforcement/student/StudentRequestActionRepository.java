package com.CLMTZ.Backend.repository.reinforcement.student;

import com.CLMTZ.Backend.dto.reinforcement.student.StudentCancelRequestResponseDTO;

public interface StudentRequestActionRepository {
    StudentCancelRequestResponseDTO cancelRequest(Integer userId, Integer requestId);
}