package com.CLMTZ.Backend.service.security.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.CLMTZ.Backend.dto.security.Request.RoleManagementRequestDTO;
import com.CLMTZ.Backend.dto.security.Response.FlatRoleMappingDTO;
import com.CLMTZ.Backend.dto.security.Response.KpiDashboardManagementResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.RoleListManagementConectionResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.RoleListManagementResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.RoleListResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;
import com.CLMTZ.Backend.model.security.RoleManagement;
import com.CLMTZ.Backend.repository.security.IRoleManagementRepository;
import com.CLMTZ.Backend.repository.security.IUserManagementRepository;
import com.CLMTZ.Backend.repository.security.custom.IRoleManagementCustomRepository;
import com.CLMTZ.Backend.service.security.IRoleManagementService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoleManagementServiceImpl implements IRoleManagementService {

    private final IRoleManagementRepository roleManagementRepo;
    private final IRoleManagementCustomRepository roleManagementCustomRepo;
    private final IUserManagementRepository userManagementRepo;

    @Override
    @Transactional(readOnly = true)
    public List<RoleManagementRequestDTO> listRoleNames(){

        List<RoleManagement> rolesEntity = roleManagementRepo.findByStateTrue();

        return rolesEntity.stream().map(rolEntity -> {
            RoleManagementRequestDTO dto = new RoleManagementRequestDTO();           
            dto.setRoleGId(rolEntity.getRoleGId()); 
            dto.setRoleG(rolEntity.getRoleG());           
            return dto;
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleListManagementResponseDTO> listRolesManagement(String filter, Boolean state){
        try{
            return roleManagementCustomRepo.listRolesManagement(filter, state);
        } catch(Exception e) {
            throw new RuntimeException("Error al cargar el listado de usuarios: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public SpResponseDTO createRoleManagement(RoleManagementRequestDTO roleRequest){
        try{
            return roleManagementCustomRepo.createRoleManagement(roleRequest.getRoleG(), roleRequest.getDescription());
        } catch (Exception e){
            throw new RuntimeException("Error al crear el rol: " + e.getMessage());
        } 
        
    }

    @Override
    @Transactional
    public SpResponseDTO updateRoleManagement(RoleManagementRequestDTO roleRequest){
        try{
            return roleManagementCustomRepo.updateRoleManagement(roleRequest.getRoleGId(), roleRequest.getRoleG(), roleRequest.getDescription(), roleRequest.getState());
        } catch (Exception e){
            throw new RuntimeException("Error al editar el rol: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleListResponseDTO> listRoleManagementRole(){
        try {
            List<FlatRoleMappingDTO> flatList = roleManagementCustomRepo.listRoleManagementRole();
            
            List<RoleListResponseDTO> listFinish = flatList.stream()
                .collect(Collectors.groupingBy(FlatRoleMappingDTO::getPidrol, LinkedHashMap::new, Collectors.toList())).entrySet().stream()
                    .map(entry -> {
                        Integer roleAppId = entry.getKey();
                        List<FlatRoleMappingDTO> mapp = entry.getValue();

                        String roleAppName = mapp.get(0).getProl();

                        List<RoleListManagementConectionResponseDTO> serverRoles = mapp.stream()
                            .map(m -> new RoleListManagementConectionResponseDTO(
                                m.getPidgrol(),
                                m.getPgrol(),
                                m.getPgdescripcion(),
                                m.getPrelacion()
                            )).collect(Collectors.toList());

                            return new RoleListResponseDTO(roleAppId, roleAppName, serverRoles);

                    }).collect(Collectors.toList());

                    return listFinish;

        } catch (Exception e) {
            throw new RuntimeException("Error al listar los roles del aplicativo: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public KpiDashboardManagementResponseDTO kpisDashboadrManagement(){

        try {

            Long userActive = userManagementRepo.countByState(true);
            Long usersInactive = userManagementRepo.countByState(false);
            Long roleActive = roleManagementRepo.countByState(true);
            Long roleInactive = roleManagementRepo.countByState(false);

            return new KpiDashboardManagementResponseDTO(userActive,usersInactive,roleActive,roleInactive);
        } catch (Exception e) {
            throw new RuntimeException("Error al cargar los datos del dashboard: " +e.getMessage());
        }
    }
}
