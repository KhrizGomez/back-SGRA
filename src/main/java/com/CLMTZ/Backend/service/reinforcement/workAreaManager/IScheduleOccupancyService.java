package com.CLMTZ.Backend.service.reinforcement.workAreaManager;

import java.util.List;

import com.CLMTZ.Backend.dto.reinforcement.workAreaManager.ScheduleOccupancyDTO;

public interface IScheduleOccupancyService {

    List<ScheduleOccupancyDTO> listScheduleOccupancies(String filterText);

}
