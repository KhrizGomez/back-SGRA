package com.CLMTZ.Backend.dto.general;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDTO {
    private Integer notificationId;
    private String title;
    private String message;
    private LocalDateTime dateSent;
    private Integer userId;
}
