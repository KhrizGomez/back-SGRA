package com.CLMTZ.Backend.controller.security;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.CLMTZ.Backend.dto.security.Request.UserRoleManagementRequestDTO;
import com.CLMTZ.Backend.dto.security.Response.KpiDashboardManagementResponseDTO;
import com.CLMTZ.Backend.service.security.IRoleManagementService;
import com.CLMTZ.Backend.service.security.IUserRoleManagementService;
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
