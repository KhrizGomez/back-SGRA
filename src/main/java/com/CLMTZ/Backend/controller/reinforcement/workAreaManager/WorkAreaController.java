package com.CLMTZ.Backend.controller.reinforcement.workAreaManager;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.CLMTZ.Backend.dto.reinforcement.WorkAreaDTO;
import com.CLMTZ.Backend.service.reinforcement.workAreaManager.IWorkAreaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reinforcement/work-areas")
@RequiredArgsConstructor
public class WorkAreaController {

    private final IWorkAreaService workAreaSer;

    @GetMapping("/list-workAreas")
    public ResponseEntity<List<WorkAreaDTO>> listWorkAreas(@RequestParam Integer academicTypeId){
        List<WorkAreaDTO> requestListAreas = workAreaSer.listAreasNames(academicTypeId);
        return ResponseEntity.ok(requestListAreas);
    }
}
