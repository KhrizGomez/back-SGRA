package com.CLMTZ.Backend.dto.reinforcement.student;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentPreferenceDTO {
    private Integer preferenceId;
    private Integer userId;
    private Integer channelId;
    private String channelName;
    private Integer reminderAnticipation;
}