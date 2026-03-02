package com.CLMTZ.Backend.service.general;

import java.util.List;

import com.CLMTZ.Backend.dto.general.NotificationDTO;

public interface INotificationService {
    List<NotificationDTO> findAll();
    NotificationDTO findById(Integer id);
    NotificationDTO save(NotificationDTO dto);
    NotificationDTO update(Integer id, NotificationDTO dto);
    void deleteById(Integer id);
}
