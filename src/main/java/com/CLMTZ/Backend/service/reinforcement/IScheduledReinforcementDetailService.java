package com.CLMTZ.Backend.service.reinforcement;

import java.util.List;
import com.CLMTZ.Backend.dto.reinforcement.ScheduledReinforcementDetailDTO;

public interface IScheduledReinforcementDetailService {
    List<ScheduledReinforcementDetailDTO> findAll();
    ScheduledReinforcementDetailDTO findById(Integer id);
    ScheduledReinforcementDetailDTO save(ScheduledReinforcementDetailDTO dto);
    ScheduledReinforcementDetailDTO update(Integer id, ScheduledReinforcementDetailDTO dto);
    void deleteById(Integer id);
}
