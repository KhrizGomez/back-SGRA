package com.CLMTZ.Backend.service.reinforcement.workAreaManager;

import java.util.List;
import com.CLMTZ.Backend.dto.reinforcement.WorkAreaResponseDTO;

public interface IWorkAreaService {
    
    List<WorkAreaResponseDTO> listWorkAreas(Integer userId, Integer workAreaTypeId);
}
