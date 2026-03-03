package com.CLMTZ.Backend.controller.reinforcement.workAreaManager;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.CLMTZ.Backend.dto.reinforcement.ListOfWorkAreaRequestsRequestDTO;
import com.CLMTZ.Backend.service.reinforcement.workAreaManager.IOnSiteReinforcementService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reinforcement/on-site-reinforcements")
@RequiredArgsConstructor
public class OnSiteReinforcementController {

    private final IOnSiteReinforcementService onSiteReinforcementSer;

    @GetMapping("/list-areas-ofsite")
    public ResponseEntity<List<ListOfWorkAreaRequestsRequestDTO>> listAreasRequests(@RequestParam Integer userId){
        List<ListOfWorkAreaRequestsRequestDTO> listAreasRequesteOfSite = onSiteReinforcementSer.listAreasRequests(userId);
        return ResponseEntity.ok(listAreasRequesteOfSite);
    }
}
