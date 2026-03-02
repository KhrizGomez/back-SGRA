package com.CLMTZ.Backend.service.reinforcement.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.CLMTZ.Backend.dto.reinforcement.ResourcesRequestsReinforcementsDTO;
import com.CLMTZ.Backend.model.reinforcement.ReinforcementRequest;
import com.CLMTZ.Backend.model.reinforcement.ResourcesRequestsReinforcements;
import com.CLMTZ.Backend.repository.reinforcement.IReinforcementRequestRepository;
import com.CLMTZ.Backend.repository.reinforcement.IResourcesRequestsReinforcementsRepository;
import com.CLMTZ.Backend.service.reinforcement.IResourcesRequestsReinforcementsService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResourcesRequestsReinforcementsServiceImpl implements IResourcesRequestsReinforcementsService {

    private final IResourcesRequestsReinforcementsRepository repository;
    private final IReinforcementRequestRepository reinforcementRequestRepository;

    @Override
    public List<ResourcesRequestsReinforcementsDTO> findAll() {
        return repository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public ResourcesRequestsReinforcementsDTO findById(Integer id) {
        return repository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("ResourcesRequestsReinforcements not found with id: " + id));
    }

    @Override
    public ResourcesRequestsReinforcementsDTO save(ResourcesRequestsReinforcementsDTO dto) {
        ResourcesRequestsReinforcements e = new ResourcesRequestsReinforcements();
        if (dto.getReinforcementRequestId() != null) {
            ReinforcementRequest rr = reinforcementRequestRepository.findById(dto.getReinforcementRequestId())
                    .orElseThrow(() -> new RuntimeException("ReinforcementRequest not found with id: " + dto.getReinforcementRequestId()));
            e.setReinforcementRequestId(rr);
        }
        e.setUrlFileRequestReinforcement(dto.getUrlFileRequestReinforcement());
        return toDTO(repository.save(e));
    }

    @Override
    public ResourcesRequestsReinforcementsDTO update(Integer id, ResourcesRequestsReinforcementsDTO dto) {
        ResourcesRequestsReinforcements e = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("ResourcesRequestsReinforcements not found with id: " + id));
        if (dto.getReinforcementRequestId() != null) {
            ReinforcementRequest rr = reinforcementRequestRepository.findById(dto.getReinforcementRequestId())
                    .orElseThrow(() -> new RuntimeException("ReinforcementRequest not found with id: " + dto.getReinforcementRequestId()));
            e.setReinforcementRequestId(rr);
        }
        e.setUrlFileRequestReinforcement(dto.getUrlFileRequestReinforcement());
        return toDTO(repository.save(e));
    }

    @Override
    public void deleteById(Integer id) {
        repository.deleteById(id);
    }

    private ResourcesRequestsReinforcementsDTO toDTO(ResourcesRequestsReinforcements e) {
        ResourcesRequestsReinforcementsDTO d = new ResourcesRequestsReinforcementsDTO();
        d.setResoucesRequestesReinforcementsId(e.getResoucesRequestesReinforcementsId());
        d.setReinforcementRequestId(e.getReinforcementRequestId() != null ? e.getReinforcementRequestId().getReinforcementRequestId() : null);
        d.setUrlFileRequestReinforcement(e.getUrlFileRequestReinforcement());
        return d;
    }
}
