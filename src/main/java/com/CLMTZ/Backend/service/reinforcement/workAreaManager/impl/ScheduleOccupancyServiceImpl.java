package com.CLMTZ.Backend.service.reinforcement.workAreaManager.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.CLMTZ.Backend.dto.reinforcement.workAreaManager.ScheduleOccupancyDTO;
import com.CLMTZ.Backend.repository.reinforcement.workAreaManager.custom.IScheduleOccupancyCustomRepository;
import com.CLMTZ.Backend.service.reinforcement.workAreaManager.IScheduleOccupancyService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ScheduleOccupancyServiceImpl implements IScheduleOccupancyService {

    private final IScheduleOccupancyCustomRepository scheduleOccupancyCustomRepo;

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleOccupancyDTO> listScheduleOccupancies(String filterText) {
        try {
            return scheduleOccupancyCustomRepo.listScheduleOccupancies(filterText);
        } catch (Exception e) {
            throw new RuntimeException("Error al listar la ocupación de horarios: " + e.getMessage());
        }
    }
}
