package com.CLMTZ.Backend.service.general.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.CLMTZ.Backend.dto.general.NotificationDTO;
import com.CLMTZ.Backend.model.general.Notification;
import com.CLMTZ.Backend.repository.general.INotificationRepository;
import com.CLMTZ.Backend.repository.general.IUserRepository;
import com.CLMTZ.Backend.service.general.INotificationService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements INotificationService {

    private final INotificationRepository repository;
    private final IUserRepository userRepository;

    @Override
    public List<NotificationDTO> findAll() { return repository.findAll().stream().map(this::toDTO).collect(Collectors.toList()); }

    @Override
    public NotificationDTO findById(Integer id) { return repository.findById(id).map(this::toDTO).orElseThrow(() -> new RuntimeException("Notification not found with id: " + id)); }

    @Override
    public NotificationDTO save(NotificationDTO dto) {
        Notification e = new Notification();
        e.setTitle(dto.getTitle()); e.setMessage(dto.getMessage()); e.setDateSent(dto.getDateSent());
        if (dto.getUserId() != null) e.setUserId(userRepository.findById(dto.getUserId()).orElseThrow(() -> new RuntimeException("User not found")));
        return toDTO(repository.save(e));
    }

    @Override
    public NotificationDTO update(Integer id, NotificationDTO dto) {
        Notification e = repository.findById(id).orElseThrow(() -> new RuntimeException("Notification not found with id: " + id));
        e.setTitle(dto.getTitle()); e.setMessage(dto.getMessage()); e.setDateSent(dto.getDateSent());
        if (dto.getUserId() != null) e.setUserId(userRepository.findById(dto.getUserId()).orElseThrow(() -> new RuntimeException("User not found")));
        return toDTO(repository.save(e));
    }

    @Override
    public void deleteById(Integer id) { repository.deleteById(id); }

    private NotificationDTO toDTO(Notification e) {
        NotificationDTO d = new NotificationDTO(); d.setNotificationId(e.getNotificationId()); d.setTitle(e.getTitle()); d.setMessage(e.getMessage()); d.setDateSent(e.getDateSent());
        d.setUserId(e.getUserId() != null ? e.getUserId().getUserId() : null); return d;
    }
}
