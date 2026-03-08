package com.CLMTZ.Backend.controller.reinforcement.workAreaManager;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.CLMTZ.Backend.dto.reinforcement.workAreaManager.ScheduleOccupancyDTO;
import com.CLMTZ.Backend.service.reinforcement.workAreaManager.IScheduleOccupancyService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reinforcement/work-area-management/schedule")
@RequiredArgsConstructor
public class ScheduleOccupancyController {

    private final IScheduleOccupancyService scheduleOccupancySer;

    @GetMapping("/occupancies")
    public ResponseEntity<List<ScheduleOccupancyDTO>> listScheduleOccupancies(
            @RequestParam(required = false, defaultValue = "") String filterText) {
        List<ScheduleOccupancyDTO> occupancies = scheduleOccupancySer.listScheduleOccupancies(filterText);
        return ResponseEntity.ok(occupancies);
    }
}
