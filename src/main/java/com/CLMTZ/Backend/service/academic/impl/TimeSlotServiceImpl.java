package com.CLMTZ.Backend.service.academic.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.CLMTZ.Backend.dto.academic.TimeSlotDTO;
import com.CLMTZ.Backend.model.academic.TimeSlot;
import com.CLMTZ.Backend.repository.academic.ITimeSlotRepository;
import com.CLMTZ.Backend.service.academic.ITimeSlotService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TimeSlotServiceImpl implements ITimeSlotService {

    private final ITimeSlotRepository repository;

    @Override
    public List<TimeSlotDTO> findAll() {
        return repository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public TimeSlotDTO findById(Integer id) {
        return repository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("TimeSlot not found with id: " + id));
    }

    @Override
    public TimeSlotDTO save(TimeSlotDTO dto) {
        TimeSlot entity = new TimeSlot();
        entity.setStartTime(dto.getStartTime());
        entity.setEndTime(dto.getEndTime());
        entity.setState(dto.getState() != null ? dto.getState() : true);
        return toDTO(repository.save(entity));
    }

    @Override
    public TimeSlotDTO update(Integer id, TimeSlotDTO dto) {
        TimeSlot entity = repository.findById(id).orElseThrow(() -> new RuntimeException("TimeSlot not found with id: " + id));
        entity.setStartTime(dto.getStartTime());
        entity.setEndTime(dto.getEndTime());
        entity.setState(dto.getState());
        return toDTO(repository.save(entity));
    }

    @Override
    public void deleteById(Integer id) { repository.deleteById(id); }

    private TimeSlotDTO toDTO(TimeSlot e) {
        TimeSlotDTO dto = new TimeSlotDTO();
        dto.setTimeSlotId(e.getTimeSlotId());
        dto.setStartTime(e.getStartTime());
        dto.setEndTime(e.getEndTime());
        dto.setState(e.getState());
        return dto;
    }
}
