package com.CLMTZ.Backend.service.reinforcement;

import java.util.List;
import com.CLMTZ.Backend.dto.reinforcement.WorkAreaDTO;

public interface IWorkAreaService {
    List<WorkAreaDTO> findAll();
    WorkAreaDTO findById(Integer id);
    WorkAreaDTO save(WorkAreaDTO dto);
    WorkAreaDTO update(Integer id, WorkAreaDTO dto);
    void deleteById(Integer id);

    List<WorkAreaDTO> listAreasNames(Integer academicid);
}
