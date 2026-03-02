package com.CLMTZ.Backend.dto.general;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationChannelsDTO {
    private Integer notificationChannelId;
    private String nameChannel;
    private Boolean state;
}
