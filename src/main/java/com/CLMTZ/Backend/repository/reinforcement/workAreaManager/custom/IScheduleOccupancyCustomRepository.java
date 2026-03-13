package com.CLMTZ.Backend.repository.reinforcement.workAreaManager.custom;

import java.util.List;

import com.CLMTZ.Backend.dto.reinforcement.workAreaManager.ScheduleOccupancyDTO;

public interface IScheduleOccupancyCustomRepository {

    List<ScheduleOccupancyDTO> listScheduleOccupancies(String filterText);

}
