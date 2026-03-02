package com.CLMTZ.Backend.service.general;

import java.util.List;

import com.CLMTZ.Backend.dto.general.NotificationChannelsDTO;

public interface INotificationChannelsService {
    List<NotificationChannelsDTO> findAll();
    NotificationChannelsDTO findById(Integer id);
    NotificationChannelsDTO save(NotificationChannelsDTO dto);
    NotificationChannelsDTO update(Integer id, NotificationChannelsDTO dto);
    void deleteById(Integer id);
}
