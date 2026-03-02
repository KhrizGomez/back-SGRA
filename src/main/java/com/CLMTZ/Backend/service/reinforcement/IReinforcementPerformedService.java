package com.CLMTZ.Backend.service.reinforcement;

import java.util.List;
import com.CLMTZ.Backend.dto.reinforcement.ReinforcementPerformedDTO;

public interface IReinforcementPerformedService {
    List<ReinforcementPerformedDTO> findAll();
    ReinforcementPerformedDTO findById(Integer id);
    ReinforcementPerformedDTO save(ReinforcementPerformedDTO dto);
    ReinforcementPerformedDTO update(Integer id, ReinforcementPerformedDTO dto);
    void deleteById(Integer id);
}
