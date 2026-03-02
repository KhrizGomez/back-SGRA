package com.CLMTZ.Backend.service.reinforcement;

import java.util.List;
import com.CLMTZ.Backend.dto.reinforcement.ScheduledReinforcementStatusDTO;

public interface IScheduledReinforcementStatusService {
    List<ScheduledReinforcementStatusDTO> findAll();
    ScheduledReinforcementStatusDTO findById(Integer id);
    ScheduledReinforcementStatusDTO save(ScheduledReinforcementStatusDTO dto);
    ScheduledReinforcementStatusDTO update(Integer id, ScheduledReinforcementStatusDTO dto);
    void deleteById(Integer id);
}
