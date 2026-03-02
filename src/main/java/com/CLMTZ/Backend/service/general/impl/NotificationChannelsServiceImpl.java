package com.CLMTZ.Backend.service.general.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.CLMTZ.Backend.dto.general.NotificationChannelsDTO;
import com.CLMTZ.Backend.model.general.NotificationChannels;
import com.CLMTZ.Backend.repository.general.INotificationChannelsRepository;
import com.CLMTZ.Backend.service.general.INotificationChannelsService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationChannelsServiceImpl implements INotificationChannelsService {

    private final INotificationChannelsRepository repository;

    @Override
    public List<NotificationChannelsDTO> findAll() { return repository.findAll().stream().map(this::toDTO).collect(Collectors.toList()); }

    @Override
    public NotificationChannelsDTO findById(Integer id) { return repository.findById(id).map(this::toDTO).orElseThrow(() -> new RuntimeException("NotificationChannel not found with id: " + id)); }

    @Override
    public NotificationChannelsDTO save(NotificationChannelsDTO dto) {
        NotificationChannels e = new NotificationChannels(); e.setNameChannel(dto.getNameChannel()); e.setState(dto.getState() != null ? dto.getState() : true);
        return toDTO(repository.save(e));
    }

    @Override
    public NotificationChannelsDTO update(Integer id, NotificationChannelsDTO dto) {
        NotificationChannels e = repository.findById(id).orElseThrow(() -> new RuntimeException("NotificationChannel not found with id: " + id));
        e.setNameChannel(dto.getNameChannel()); e.setState(dto.getState());
        return toDTO(repository.save(e));
    }

    @Override
    public void deleteById(Integer id) { repository.deleteById(id); }

    private NotificationChannelsDTO toDTO(NotificationChannels e) { NotificationChannelsDTO d = new NotificationChannelsDTO(); d.setNotificationChannelId(e.getNotificationChannelId()); d.setNameChannel(e.getNameChannel()); d.setState(e.getState()); return d; }
}
