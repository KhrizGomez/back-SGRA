package com.CLMTZ.Backend.service.reinforcement;

import java.util.List;
import com.CLMTZ.Backend.dto.reinforcement.SessionTypesDTO;

public interface ISessionTypesService {
    List<SessionTypesDTO> findAll();
    SessionTypesDTO findById(Integer id);
    SessionTypesDTO save(SessionTypesDTO dto);
    SessionTypesDTO update(Integer id, SessionTypesDTO dto);
    void deleteById(Integer id);
}
