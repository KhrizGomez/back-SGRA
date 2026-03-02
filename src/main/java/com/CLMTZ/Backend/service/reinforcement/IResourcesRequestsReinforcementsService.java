package com.CLMTZ.Backend.service.reinforcement;

import java.util.List;
import com.CLMTZ.Backend.dto.reinforcement.ResourcesRequestsReinforcementsDTO;

public interface IResourcesRequestsReinforcementsService {
    List<ResourcesRequestsReinforcementsDTO> findAll();
    ResourcesRequestsReinforcementsDTO findById(Integer id);
    ResourcesRequestsReinforcementsDTO save(ResourcesRequestsReinforcementsDTO dto);
    ResourcesRequestsReinforcementsDTO update(Integer id, ResourcesRequestsReinforcementsDTO dto);
    void deleteById(Integer id);
}
