package com.CLMTZ.Backend.service.reinforcement.teacher;

import com.CLMTZ.Backend.dto.reinforcement.TeacherAvailabilityBatchDTO;
import com.CLMTZ.Backend.dto.reinforcement.TeacherAvailabilitySlotDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherActionResponseDTO;

import java.util.List;

public interface TeacherAvailabilityService {
    List<TeacherAvailabilitySlotDTO> getMyAvailability(Integer userId, Integer periodId);
    TeacherActionResponseDTO saveMyAvailability(Integer userId, Integer periodId, List<TeacherAvailabilityBatchDTO.SlotDTO> slots);
    List<TeacherAvailabilitySlotDTO> getAvailabilityForStudent(Integer teacherId, Integer periodId);
}