package com.CLMTZ.Backend.service.reinforcement.workAreaManager;

import java.util.List;
import com.CLMTZ.Backend.dto.reinforcement.WorkAreaDTO;

public interface IWorkAreaService {
    
    List<WorkAreaDTO> listAreasNames(Integer academicid);
}
