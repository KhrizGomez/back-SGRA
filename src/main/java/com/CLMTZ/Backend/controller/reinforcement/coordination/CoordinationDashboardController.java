package com.CLMTZ.Backend.controller.reinforcement.coordination;

import com.CLMTZ.Backend.dto.reinforcement.coordination.CoordinationDashboardDTO;
import com.CLMTZ.Backend.service.reinforcement.coordination.CoordinationDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coordination/dashboard")
@RequiredArgsConstructor
public class CoordinationDashboardController {

    private final CoordinationDashboardService coordinationDashboardService;

    @GetMapping
    public ResponseEntity<CoordinationDashboardDTO> getDashboard() {
        return ResponseEntity.ok(coordinationDashboardService.getDashboard());
    }
}
