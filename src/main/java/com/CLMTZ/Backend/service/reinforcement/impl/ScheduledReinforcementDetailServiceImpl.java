package com.CLMTZ.Backend.service.reinforcement.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.CLMTZ.Backend.dto.reinforcement.ScheduledReinforcementDetailDTO;
import com.CLMTZ.Backend.model.reinforcement.ScheduledReinforcementDetail;
import com.CLMTZ.Backend.repository.reinforcement.IScheduledReinforcementDetailRepository;
import com.CLMTZ.Backend.repository.reinforcement.IScheduledReinforcementRepository;
import com.CLMTZ.Backend.repository.reinforcement.IReinforcementRequestRepository;
import com.CLMTZ.Backend.service.reinforcement.IScheduledReinforcementDetailService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ScheduledReinforcementDetailServiceImpl implements IScheduledReinforcementDetailService {

    private final IScheduledReinforcementDetailRepository repository;
    private final IScheduledReinforcementRepository scheduledReinforcementRepository;
    private final IReinforcementRequestRepository reinforcementRequestRepository;

    @Override
    public List<ScheduledReinforcementDetailDTO> findAll() { return repository.findAll().stream().map(this::toDTO).collect(Collectors.toList()); }

    @Override
    public ScheduledReinforcementDetailDTO findById(Integer id) { return repository.findById(id).map(this::toDTO).orElseThrow(() -> new RuntimeException("ScheduledReinforcementDetail not found with id: " + id)); }

    @Override
    public ScheduledReinforcementDetailDTO save(ScheduledReinforcementDetailDTO dto) {
        ScheduledReinforcementDetail e = new ScheduledReinforcementDetail();
        e.setState(dto.getState());
        if (dto.getScheduledReinforcementId() != null) e.setScheduledReinforcementId(scheduledReinforcementRepository.findById(dto.getScheduledReinforcementId()).orElseThrow(() -> new RuntimeException("ScheduledReinforcement not found")));
        if (dto.getReinforcementRequestId() != null) e.setReinforcementRequestId(reinforcementRequestRepository.findById(dto.getReinforcementRequestId()).orElseThrow(() -> new RuntimeException("ReinforcementRequest not found")));
        return toDTO(repository.save(e));
    }

    @Override
    public ScheduledReinforcementDetailDTO update(Integer id, ScheduledReinforcementDetailDTO dto) {
        ScheduledReinforcementDetail e = repository.findById(id).orElseThrow(() -> new RuntimeException("ScheduledReinforcementDetail not found with id: " + id));
        e.setState(dto.getState());
        if (dto.getScheduledReinforcementId() != null) e.setScheduledReinforcementId(scheduledReinforcementRepository.findById(dto.getScheduledReinforcementId()).orElseThrow(() -> new RuntimeException("ScheduledReinforcement not found")));
        if (dto.getReinforcementRequestId() != null) e.setReinforcementRequestId(reinforcementRequestRepository.findById(dto.getReinforcementRequestId()).orElseThrow(() -> new RuntimeException("ReinforcementRequest not found")));
        return toDTO(repository.save(e));
    }

    @Override
    public void deleteById(Integer id) { repository.deleteById(id); }

    private ScheduledReinforcementDetailDTO toDTO(ScheduledReinforcementDetail e) {
        ScheduledReinforcementDetailDTO d = new ScheduledReinforcementDetailDTO();
        d.setScheduledReinforcementDetailId(e.getScheduledReinforcementDetailId()); d.setState(e.getState());
        d.setScheduledReinforcementId(e.getScheduledReinforcementId() != null ? e.getScheduledReinforcementId().getScheduledReinforcementId() : null);
        d.setReinforcementRequestId(e.getReinforcementRequestId() != null ? e.getReinforcementRequestId().getReinforcementRequestId() : null);
        return d;
    }
}
