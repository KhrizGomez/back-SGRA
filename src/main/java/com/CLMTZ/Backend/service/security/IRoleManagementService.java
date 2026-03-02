package com.CLMTZ.Backend.service.security;

import java.util.List;

import com.CLMTZ.Backend.dto.security.Request.RoleManagementRequestDTO;
import com.CLMTZ.Backend.dto.security.Request.RoleRequestDTO;
import com.CLMTZ.Backend.dto.security.Response.KpiDashboardManagementResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.RoleListManagementResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.RoleListResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;

public interface IRoleManagementService {
    SpResponseDTO createRoleManagement(RoleManagementRequestDTO roleRequest);

    SpResponseDTO updateRoleManagement(RoleManagementRequestDTO rolRequest);

    List<RoleListManagementResponseDTO> listRolesManagement(String filter, Boolean state);

    List<RoleManagementRequestDTO> listRoleNames();

    KpiDashboardManagementResponseDTO kpisDashboadrManagement();

    List<RoleListResponseDTO> listRoleManagementRole();
}
