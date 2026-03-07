package com.CLMTZ.Backend.controller.reinforcement.workAreaManager;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.CLMTZ.Backend.dto.reinforcement.WorkAreaResponseDTO;
import com.CLMTZ.Backend.dto.reinforcement.workAreaManager.AssignWorkAreaReinforcementDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;
import com.CLMTZ.Backend.service.reinforcement.workAreaManager.IWorkAreaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reinforcement/work-areas")
@RequiredArgsConstructor
public class WorkAreaController {

    private final IWorkAreaService workAreaSer;

    @GetMapping("/list-workAreas")
    public ResponseEntity<List<WorkAreaResponseDTO>> listWorkAreas(@RequestParam Integer userId, @RequestParam Integer workAreaTypeId){
        List<WorkAreaResponseDTO> requestListAreas = workAreaSer.listWorkAreas(userId,workAreaTypeId);
        return ResponseEntity.ok(requestListAreas);
    }

    @PutMapping("/assign-work-area")
    public ResponseEntity<SpResponseDTO> AssignWorkAreaReinforcement (@RequestBody AssignWorkAreaReinforcementDTO assignWorkAreaReinforcement){
        SpResponseDTO requestAssign = workAreaSer.AssignWorkAreaReinforcement(assignWorkAreaReinforcement);
        return ResponseEntity.ok(requestAssign);
    }
}
