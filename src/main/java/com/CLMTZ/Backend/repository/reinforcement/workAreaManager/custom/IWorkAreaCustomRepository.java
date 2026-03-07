package com.CLMTZ.Backend.repository.reinforcement.workAreaManager.custom;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.CLMTZ.Backend.dto.reinforcement.WorkAreaResponseDTO;
import com.CLMTZ.Backend.dto.reinforcement.workAreaManager.AssignWorkAreaReinforcementDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;

@Repository
public interface IWorkAreaCustomRepository {
    List<WorkAreaResponseDTO> listWorkAreas(Integer userId, Integer workAreaTypeId);

    SpResponseDTO AssignWorkAreaReinforcement (AssignWorkAreaReinforcementDTO assignWorkAreaReinforcement);
}
