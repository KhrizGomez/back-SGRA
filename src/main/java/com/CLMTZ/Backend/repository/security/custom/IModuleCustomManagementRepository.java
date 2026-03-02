package com.CLMTZ.Backend.repository.security.custom;

import java.util.List;

import com.CLMTZ.Backend.dto.security.Request.MasterDataManagementRequestDTO;
import com.CLMTZ.Backend.dto.security.Request.MasterManagementRequestDTO;
import com.CLMTZ.Backend.dto.security.Response.MasterDataListManagementResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.MasterTableListManagementResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.ModuleListManagementResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;

public interface IModuleCustomManagementRepository {
    List<ModuleListManagementResponseDTO> listModuleManagements(String grole);

    List<MasterTableListManagementResponseDTO> listMasterTables();

    List<MasterDataListManagementResponseDTO> listDataMasterTables(String schemaTable, String filtro);

    SpResponseDTO updateRolePermissions(String jsonPermissions);

    SpResponseDTO masterTablesManagement(MasterManagementRequestDTO masterTables);

    SpResponseDTO masterDataUpdateManagement(MasterDataManagementRequestDTO dataUpdate);
}
