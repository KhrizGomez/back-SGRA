package com.CLMTZ.Backend.service.general.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.CLMTZ.Backend.dto.general.PreferenceDTO;
import com.CLMTZ.Backend.model.general.Preference;
import com.CLMTZ.Backend.repository.general.IPreferenceRepository;
import com.CLMTZ.Backend.repository.general.IUserRepository;
import com.CLMTZ.Backend.repository.general.INotificationChannelsRepository;
import com.CLMTZ.Backend.service.general.IPreferenceService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PreferenceServiceImpl implements IPreferenceService {

    private final IPreferenceRepository repository;
    private final IUserRepository userRepository;
    private final INotificationChannelsRepository notificationChannelsRepository;

    @Override
    public List<PreferenceDTO> findAll() { return repository.findAll().stream().map(this::toDTO).collect(Collectors.toList()); }

    @Override
    public PreferenceDTO findById(Integer id) { return repository.findById(id).map(this::toDTO).orElseThrow(() -> new RuntimeException("Preference not found with id: " + id)); }

    @Override
    public PreferenceDTO save(PreferenceDTO dto) {
        Preference e = new Preference();
        e.setReminderAdvance(dto.getReminderAdvance());
        if (dto.getUserId() != null) e.setUserId(userRepository.findById(dto.getUserId()).orElseThrow(() -> new RuntimeException("User not found")));
        if (dto.getNotificationChannelId() != null) e.setNotificationChannelId(notificationChannelsRepository.findById(dto.getNotificationChannelId()).orElseThrow(() -> new RuntimeException("NotificationChannel not found")));
        return toDTO(repository.save(e));
    }

    @Override
    public PreferenceDTO update(Integer id, PreferenceDTO dto) {
        Preference e = repository.findById(id).orElseThrow(() -> new RuntimeException("Preference not found with id: " + id));
        e.setReminderAdvance(dto.getReminderAdvance());
        if (dto.getUserId() != null) e.setUserId(userRepository.findById(dto.getUserId()).orElseThrow(() -> new RuntimeException("User not found")));
        if (dto.getNotificationChannelId() != null) e.setNotificationChannelId(notificationChannelsRepository.findById(dto.getNotificationChannelId()).orElseThrow(() -> new RuntimeException("NotificationChannel not found")));
        return toDTO(repository.save(e));
    }

    @Override
    public void deleteById(Integer id) { repository.deleteById(id); }

    private PreferenceDTO toDTO(Preference e) {
        PreferenceDTO d = new PreferenceDTO();
        d.setPreferenceId(e.getPreferenceId()); d.setReminderAdvance(e.getReminderAdvance());
        d.setUserId(e.getUserId() != null ? e.getUserId().getUserId() : null);
        d.setNotificationChannelId(e.getNotificationChannelId() != null ? e.getNotificationChannelId().getNotificationChannelId() : null);
        return d;
    }
}
