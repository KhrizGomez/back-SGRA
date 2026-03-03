package com.CLMTZ.Backend.controller.security;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.CLMTZ.Backend.dto.security.Response.KpiDashboardManagementResponseDTO;
import com.CLMTZ.Backend.service.security.IRoleManagementService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/security/user-role-managements")
@RequiredArgsConstructor
public class UserRoleManagementController {
    
    private final IRoleManagementService roleManagementSer;

    @GetMapping("/kpi-dashboard-management")
    public ResponseEntity<KpiDashboardManagementResponseDTO> kpiDashboardManagement(){
        KpiDashboardManagementResponseDTO kpidashboard = roleManagementSer.kpisDashboadrManagement();
        return ResponseEntity.ok(kpidashboard);
    }    
}
