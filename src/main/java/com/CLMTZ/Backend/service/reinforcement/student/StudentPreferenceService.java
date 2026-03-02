package com.CLMTZ.Backend.service.reinforcement.student;

import com.CLMTZ.Backend.dto.reinforcement.student.NotificationChannelDTO;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentPreferenceDTO;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentPreferenceUpsertRequestDTO;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentPreferenceUpsertResponseDTO;

import java.util.List;

public interface StudentPreferenceService {
    List<NotificationChannelDTO> getActiveChannels();
    StudentPreferenceDTO getMyPreference(Integer userId);
    StudentPreferenceUpsertResponseDTO saveMyPreference(Integer userId, StudentPreferenceUpsertRequestDTO req);
}