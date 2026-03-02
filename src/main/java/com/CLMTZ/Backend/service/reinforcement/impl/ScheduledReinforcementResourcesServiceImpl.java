package com.CLMTZ.Backend.service.reinforcement.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.CLMTZ.Backend.dto.reinforcement.ScheduledReinforcementResourcesDTO;
import com.CLMTZ.Backend.model.reinforcement.ScheduledReinforcement;
import com.CLMTZ.Backend.model.reinforcement.ScheduledReinforcementResources;
import com.CLMTZ.Backend.repository.reinforcement.IScheduledReinforcementRepository;
import com.CLMTZ.Backend.repository.reinforcement.IScheduledReinforcementResourcesRepository;
import com.CLMTZ.Backend.service.reinforcement.IScheduledReinforcementResourcesService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ScheduledReinforcementResourcesServiceImpl implements IScheduledReinforcementResourcesService {

    private final IScheduledReinforcementResourcesRepository repository;
    private final IScheduledReinforcementRepository scheduledReinforcementRepository;

    @Override
    public List<ScheduledReinforcementResourcesDTO> findAll() {
        return repository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public ScheduledReinforcementResourcesDTO findById(Integer id) {
        return repository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("ScheduledReinforcementResources not found with id: " + id));
    }

    @Override
    public ScheduledReinforcementResourcesDTO save(ScheduledReinforcementResourcesDTO dto) {
        ScheduledReinforcementResources e = new ScheduledReinforcementResources();
        if (dto.getScheduledReinforcementId() != null) {
            ScheduledReinforcement sr = scheduledReinforcementRepository.findById(dto.getScheduledReinforcementId())
                    .orElseThrow(() -> new RuntimeException("ScheduledReinforcement not found with id: " + dto.getScheduledReinforcementId()));
            e.setScheduledReinforcement(sr);
        }
        e.setUrlFileScheduledReinforcement(dto.getUrlFileScheduledReinforcement());
        return toDTO(repository.save(e));
    }

    @Override
    public ScheduledReinforcementResourcesDTO update(Integer id, ScheduledReinforcementResourcesDTO dto) {
        ScheduledReinforcementResources e = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("ScheduledReinforcementResources not found with id: " + id));
        if (dto.getScheduledReinforcementId() != null) {
            ScheduledReinforcement sr = scheduledReinforcementRepository.findById(dto.getScheduledReinforcementId())
                    .orElseThrow(() -> new RuntimeException("ScheduledReinforcement not found with id: " + dto.getScheduledReinforcementId()));
            e.setScheduledReinforcement(sr);
        }
        e.setUrlFileScheduledReinforcement(dto.getUrlFileScheduledReinforcement());
        return toDTO(repository.save(e));
    }

    @Override
    public void deleteById(Integer id) {
        repository.deleteById(id);
    }

    private ScheduledReinforcementResourcesDTO toDTO(ScheduledReinforcementResources e) {
        ScheduledReinforcementResourcesDTO d = new ScheduledReinforcementResourcesDTO();
        d.setScheduledReinforcementResourcesId(e.getScheduledReinforcementResourcesId());
        d.setScheduledReinforcementId(e.getScheduledReinforcement() != null ? e.getScheduledReinforcement().getScheduledReinforcementId() : null);
        d.setUrlFileScheduledReinforcement(e.getUrlFileScheduledReinforcement());
        return d;
    }
}
