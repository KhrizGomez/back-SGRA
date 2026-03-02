package com.CLMTZ.Backend.service.reinforcement.student.impl;

import com.CLMTZ.Backend.dto.reinforcement.student.NotificationChannelDTO;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentPreferenceDTO;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentPreferenceUpsertRequestDTO;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentPreferenceUpsertResponseDTO;
import com.CLMTZ.Backend.repository.reinforcement.student.StudentPreferenceRepository;
import com.CLMTZ.Backend.service.reinforcement.student.StudentPreferenceService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentPreferenceServiceImpl implements StudentPreferenceService {

    private final StudentPreferenceRepository studentPreferenceRepository;

    public StudentPreferenceServiceImpl(StudentPreferenceRepository studentPreferenceRepository) {
        this.studentPreferenceRepository = studentPreferenceRepository;
    }

    @Override
    public List<NotificationChannelDTO> getActiveChannels() {
        return studentPreferenceRepository.listActiveChannels();
    }

    @Override
    public StudentPreferenceDTO getMyPreference(Integer userId) {
        return studentPreferenceRepository.getPreferenceByUser(userId);
    }

    @Override
    public StudentPreferenceUpsertResponseDTO saveMyPreference(Integer userId, StudentPreferenceUpsertRequestDTO req) {
        studentPreferenceRepository.upsertPreference(userId, req.getChannelId(), req.getReminderAnticipation());
        return new StudentPreferenceUpsertResponseDTO("Preferences saved");
    }
}