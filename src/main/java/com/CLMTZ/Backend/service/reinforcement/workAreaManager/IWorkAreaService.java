package com.CLMTZ.Backend.service.reinforcement.workAreaManager;

import java.util.List;
import com.CLMTZ.Backend.dto.reinforcement.WorkAreaResponseDTO;
import com.CLMTZ.Backend.dto.reinforcement.workAreaManager.AssignWorkAreaReinforcementDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;

public interface IWorkAreaService {
    
    List<WorkAreaResponseDTO> listWorkAreas(Integer userId, Integer workAreaTypeId, Integer ReinforcementId);

    SpResponseDTO AssignWorkAreaReinforcement (AssignWorkAreaReinforcementDTO assignWorkAreaReinforcement);
}
