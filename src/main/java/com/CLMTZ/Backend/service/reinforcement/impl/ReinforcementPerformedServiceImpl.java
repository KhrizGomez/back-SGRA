package com.CLMTZ.Backend.service.reinforcement.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.CLMTZ.Backend.dto.reinforcement.ReinforcementPerformedDTO;
import com.CLMTZ.Backend.model.reinforcement.ReinforcementPerformed;
import com.CLMTZ.Backend.repository.reinforcement.IReinforcementPerformedRepository;
import com.CLMTZ.Backend.repository.reinforcement.IScheduledReinforcementRepository;
import com.CLMTZ.Backend.service.reinforcement.IReinforcementPerformedService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReinforcementPerformedServiceImpl implements IReinforcementPerformedService {

    private final IReinforcementPerformedRepository repository;
    private final IScheduledReinforcementRepository scheduledReinforcementRepository;

    @Override
    public List<ReinforcementPerformedDTO> findAll() { return repository.findAll().stream().map(this::toDTO).collect(Collectors.toList()); }

    @Override
    public ReinforcementPerformedDTO findById(Integer id) { return repository.findById(id).map(this::toDTO).orElseThrow(() -> new RuntimeException("ReinforcementPerformed not found with id: " + id)); }

    @Override
    public ReinforcementPerformedDTO save(ReinforcementPerformedDTO dto) {
        ReinforcementPerformed e = new ReinforcementPerformed();
        e.setObservation(dto.getObservation()); e.setDuration(dto.getDuration()); e.setState(dto.getState());
        if (dto.getScheduledReinforcementId() != null) e.setScheduledReinforcementId(scheduledReinforcementRepository.findById(dto.getScheduledReinforcementId()).orElseThrow(() -> new RuntimeException("ScheduledReinforcement not found")));
        return toDTO(repository.save(e));
    }

    @Override
    public ReinforcementPerformedDTO update(Integer id, ReinforcementPerformedDTO dto) {
        ReinforcementPerformed e = repository.findById(id).orElseThrow(() -> new RuntimeException("ReinforcementPerformed not found with id: " + id));
        e.setObservation(dto.getObservation()); e.setDuration(dto.getDuration()); e.setState(dto.getState());
        if (dto.getScheduledReinforcementId() != null) e.setScheduledReinforcementId(scheduledReinforcementRepository.findById(dto.getScheduledReinforcementId()).orElseThrow(() -> new RuntimeException("ScheduledReinforcement not found")));
        return toDTO(repository.save(e));
    }

    @Override
    public void deleteById(Integer id) { repository.deleteById(id); }

    private ReinforcementPerformedDTO toDTO(ReinforcementPerformed e) {
        ReinforcementPerformedDTO d = new ReinforcementPerformedDTO();
        d.setReinforcementPerformedId(e.getReinforcementPerformedId()); d.setObservation(e.getObservation()); d.setDuration(e.getDuration()); d.setState(e.getState());
        d.setScheduledReinforcementId(e.getScheduledReinforcementId() != null ? e.getScheduledReinforcementId().getScheduledReinforcementId() : null);
        return d;
    }
}
