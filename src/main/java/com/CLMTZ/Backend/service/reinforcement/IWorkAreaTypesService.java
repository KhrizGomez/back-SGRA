package com.CLMTZ.Backend.service.reinforcement;

import java.util.List;
import com.CLMTZ.Backend.dto.reinforcement.WorkAreaTypesDTO;

public interface IWorkAreaTypesService {
    List<WorkAreaTypesDTO> findAll();
    WorkAreaTypesDTO findById(Integer id);
    WorkAreaTypesDTO save(WorkAreaTypesDTO dto);
    WorkAreaTypesDTO update(Integer id, WorkAreaTypesDTO dto);
    void deleteById(Integer id);
}
