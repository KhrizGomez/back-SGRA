package com.CLMTZ.Backend.dto.general;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PreferenceDTO {
    private Integer preferenceId;
    private Integer reminderAdvance;
    private Integer userId;
    private Integer notificationChannelId;
}
