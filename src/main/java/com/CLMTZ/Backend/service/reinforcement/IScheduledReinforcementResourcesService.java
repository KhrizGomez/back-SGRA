package com.CLMTZ.Backend.service.reinforcement;

import java.util.List;
import com.CLMTZ.Backend.dto.reinforcement.ScheduledReinforcementResourcesDTO;

public interface IScheduledReinforcementResourcesService {
    List<ScheduledReinforcementResourcesDTO> findAll();
    ScheduledReinforcementResourcesDTO findById(Integer id);
    ScheduledReinforcementResourcesDTO save(ScheduledReinforcementResourcesDTO dto);
    ScheduledReinforcementResourcesDTO update(Integer id, ScheduledReinforcementResourcesDTO dto);
    void deleteById(Integer id);
}
