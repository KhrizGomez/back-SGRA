package com.CLMTZ.Backend.service.reinforcement.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.CLMTZ.Backend.dto.reinforcement.ScheduledReinforcementStatusDTO;
import com.CLMTZ.Backend.model.reinforcement.ScheduledReinforcementStatus;
import com.CLMTZ.Backend.repository.reinforcement.jpa.IScheduledReinforcementStatusRepository;
import com.CLMTZ.Backend.service.reinforcement.IScheduledReinforcementStatusService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ScheduledReinforcementStatusServiceImpl implements IScheduledReinforcementStatusService {

    private final IScheduledReinforcementStatusRepository repository;

    @Override
    public List<ScheduledReinforcementStatusDTO> findAll() {
        return repository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public ScheduledReinforcementStatusDTO findById(Integer id) {
        return repository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("ScheduledReinforcementStatus not found with id: " + id));
    }

    @Override
    public ScheduledReinforcementStatusDTO save(ScheduledReinforcementStatusDTO dto) {
        ScheduledReinforcementStatus e = new ScheduledReinforcementStatus();
        e.setScheduledReinforcementStatus(dto.getScheduledReinforcementStatus());
        e.setState(dto.getState() != null ? dto.getState() : true);
        return toDTO(repository.save(e));
    }

    @Override
    public ScheduledReinforcementStatusDTO update(Integer id, ScheduledReinforcementStatusDTO dto) {
        ScheduledReinforcementStatus e = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("ScheduledReinforcementStatus not found with id: " + id));
        e.setScheduledReinforcementStatus(dto.getScheduledReinforcementStatus());
        e.setState(dto.getState());
        return toDTO(repository.save(e));
    }

    @Override
    public void deleteById(Integer id) {
        repository.deleteById(id);
    }

    private ScheduledReinforcementStatusDTO toDTO(ScheduledReinforcementStatus e) {
        ScheduledReinforcementStatusDTO d = new ScheduledReinforcementStatusDTO();
        d.setScheduledReinforcementStatusId(e.getScheduledReinforcementStatusId());
        d.setScheduledReinforcementStatus(e.getScheduledReinforcementStatus());
        d.setState(e.getState());
        return d;
    }
}
