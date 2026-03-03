package com.CLMTZ.Backend.service.security;

import java.util.List;

import com.CLMTZ.Backend.dto.security.Request.MasterDataManagementRequestDTO;
import com.CLMTZ.Backend.dto.security.Request.MasterManagementRequestDTO;
import com.CLMTZ.Backend.dto.security.Request.UpdateRolePermissionsRequestDTO;
import com.CLMTZ.Backend.dto.security.Response.MasterDataListManagementResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.MasterTableListManagementResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.ModuleListManagementResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;

public interface IModuleManagementService {

    List<ModuleListManagementResponseDTO> listModuleManagements(String role);

    List<MasterTableListManagementResponseDTO> listMasterTables();

    List<MasterDataListManagementResponseDTO> listDataMasterTables(String schemaTables, String filter);

    SpResponseDTO updateRolePermissions(UpdateRolePermissionsRequestDTO updateRolesPermissions);

    SpResponseDTO masterTablesManagement(MasterManagementRequestDTO masterTables);

    SpResponseDTO masterDataUpdateManagement(MasterDataManagementRequestDTO dataUpdate);
    
}
