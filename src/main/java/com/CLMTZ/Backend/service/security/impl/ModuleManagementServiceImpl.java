package com.CLMTZ.Backend.service.security.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.CLMTZ.Backend.dto.security.Request.MasterDataManagementRequestDTO;
import com.CLMTZ.Backend.dto.security.Request.MasterManagementRequestDTO;
import com.CLMTZ.Backend.dto.security.Request.UpdateRolePermissionsRequestDTO;
import com.CLMTZ.Backend.dto.security.Response.MasterDataListManagementResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.MasterTableListManagementResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.ModuleListManagementResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;
import com.CLMTZ.Backend.repository.security.custom.IModuleCustomManagementRepository;
import com.CLMTZ.Backend.service.security.IModuleManagementService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ModuleManagementServiceImpl implements IModuleManagementService {

    private final ObjectMapper objectMapper;
    private final IModuleCustomManagementRepository moduleManagementCustomRepo;

    @Override
    @Transactional(readOnly = true)
    public List<ModuleListManagementResponseDTO> listModuleManagements(String grole){
        String vgrole = (grole == null) ? "" : grole;
        return moduleManagementCustomRepo.listModuleManagements(vgrole);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MasterTableListManagementResponseDTO> listMasterTables(){
        return moduleManagementCustomRepo.listMasterTables();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MasterDataListManagementResponseDTO> listDataMasterTables(String schemaTables, String filter){
        String filtro = (filter == null) ? "" : filter;
        return moduleManagementCustomRepo.listDataMasterTables(schemaTables,filtro);
    }

    @Override
    @Transactional
    public SpResponseDTO updateRolePermissions(UpdateRolePermissionsRequestDTO updateRolesPermissionsRequest){
        try {
            String jsonPermisos = objectMapper.writeValueAsString(updateRolesPermissionsRequest);

            return moduleManagementCustomRepo.updateRolePermissions(jsonPermisos);

        } catch (Exception e) {
            return new SpResponseDTO("Error al guardar los permisos: " + e.getMessage(), false);
        }
    }

    @Override
    @Transactional
    public SpResponseDTO masterTablesManagement(MasterManagementRequestDTO masterTables){
        try {
            return moduleManagementCustomRepo.masterTablesManagement(masterTables);
        } catch (Exception e) {
            return new SpResponseDTO("Error al guardar el nuevo registro de la tabla: " + e.getMessage(), false);
        }
    }

    @Override
    @Transactional
    public SpResponseDTO masterDataUpdateManagement(MasterDataManagementRequestDTO dataUpdate){
        try {
            return moduleManagementCustomRepo.masterDataUpdateManagement(dataUpdate);
        } catch (Exception e) {
            return new SpResponseDTO("Error al actualizar el registro de la tabla: " + e.getMessage(), false);
        }
    }
}
