package com.CLMTZ.Backend.repository.reinforcement.teacher;

import com.CLMTZ.Backend.dto.reinforcement.TeacherAvailabilityBatchDTO;
import com.CLMTZ.Backend.dto.reinforcement.TeacherAvailabilitySlotDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherActionResponseDTO;

import java.util.List;

public interface TeacherAvailabilityRepository {
    List<TeacherAvailabilitySlotDTO> getAvailability(Integer userId, Integer periodId);
    TeacherActionResponseDTO saveAvailability(Integer userId, Integer periodId, List<TeacherAvailabilityBatchDTO.SlotDTO> slots);
    List<TeacherAvailabilitySlotDTO> getAvailabilityForStudent(Integer teacherId, Integer periodId);
}