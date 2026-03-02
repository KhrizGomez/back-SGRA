package com.CLMTZ.Backend.service.reinforcement;

import java.util.List;
import com.CLMTZ.Backend.dto.reinforcement.ParticipantsDTO;

public interface IParticipantsService {
    List<ParticipantsDTO> findAll();
    ParticipantsDTO findById(Integer id);
    ParticipantsDTO save(ParticipantsDTO dto);
    ParticipantsDTO update(Integer id, ParticipantsDTO dto);
    void deleteById(Integer id);
}
