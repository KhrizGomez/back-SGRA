package com.CLMTZ.Backend.repository.reinforcement.student;

import com.CLMTZ.Backend.dto.reinforcement.student.NotificationChannelDTO;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentPreferenceDTO;

import java.util.List;

public interface StudentPreferenceRepository {
    List<NotificationChannelDTO> listActiveChannels();
    StudentPreferenceDTO getPreferenceByUser(Integer userId);
    void upsertPreference(Integer userId, Integer channelId, Integer reminderAnticipation);
}