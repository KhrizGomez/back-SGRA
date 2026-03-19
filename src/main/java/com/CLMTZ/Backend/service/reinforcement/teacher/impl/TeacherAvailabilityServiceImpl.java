package com.CLMTZ.Backend.service.reinforcement.teacher.impl;

import com.CLMTZ.Backend.dto.reinforcement.TeacherAvailabilityBatchDTO;
import com.CLMTZ.Backend.dto.reinforcement.TeacherAvailabilitySlotDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherActionResponseDTO;
import com.CLMTZ.Backend.repository.reinforcement.teacher.TeacherAvailabilityRepository;
import com.CLMTZ.Backend.service.reinforcement.teacher.TeacherAvailabilityService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeacherAvailabilityServiceImpl implements TeacherAvailabilityService {

    private final TeacherAvailabilityRepository teacherAvailabilityRepository;

    public TeacherAvailabilityServiceImpl(TeacherAvailabilityRepository teacherAvailabilityRepository) {
        this.teacherAvailabilityRepository = teacherAvailabilityRepository;
    }

    @Override
    public List<TeacherAvailabilitySlotDTO> getMyAvailability(Integer userId, Integer periodId) {
        return teacherAvailabilityRepository.getAvailability(userId, periodId);
    }

    @Override
    public TeacherActionResponseDTO saveMyAvailability(Integer userId, Integer periodId,
                                                       List<TeacherAvailabilityBatchDTO.SlotDTO> slots) {
        return teacherAvailabilityRepository.saveAvailability(userId, periodId, slots);
    }

    @Override
    public List<TeacherAvailabilitySlotDTO> getAvailabilityForStudent(Integer teacherId, Integer periodId) {
        return teacherAvailabilityRepository.getAvailabilityForStudent(teacherId, periodId);
    }
}