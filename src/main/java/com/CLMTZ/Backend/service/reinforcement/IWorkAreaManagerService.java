package com.CLMTZ.Backend.service.reinforcement;

import java.util.List;
import com.CLMTZ.Backend.dto.reinforcement.WorkAreaManagerDTO;

public interface IWorkAreaManagerService {
    List<WorkAreaManagerDTO> findAll();
    WorkAreaManagerDTO findById(Integer id);
    WorkAreaManagerDTO save(WorkAreaManagerDTO dto);
    WorkAreaManagerDTO update(Integer id, WorkAreaManagerDTO dto);
    void deleteById(Integer id);
}
