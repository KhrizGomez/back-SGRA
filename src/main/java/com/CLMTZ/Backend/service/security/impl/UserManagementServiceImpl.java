package com.CLMTZ.Backend.service.security.impl;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.CLMTZ.Backend.dto.security.Request.UserManagementRequestDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.UserListManagementResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.UserRoleManagementResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.UserRolesUpdateManagementResponseDTO;
import com.CLMTZ.Backend.repository.security.custom.IUserManagementCustomRepository;
import com.CLMTZ.Backend.service.security.IUserManagementService;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class UserManagementServiceImpl implements IUserManagementService {

    private final IUserManagementCustomRepository userManagementCustRepo;
    private final ObjectMapper objectMapper;


    @Override
    @Transactional(readOnly = true)
    public List<UserListManagementResponseDTO> listUserListManagement(String filterUser, LocalDate date, Boolean state){
        try {
            return userManagementCustRepo.listUsersManagement(filterUser, date, state);
        } catch (Exception e) {
            throw new RuntimeException("Error al listar a los usuarios: " + e.getMessage());
        }  
    }

    @Override
    @Transactional
    public SpResponseDTO createUserManagement(UserManagementRequestDTO userRequest){
        try {
            String rolesSep = "";
            if(userRequest.getRoles() != null && !userRequest.getRoles().isEmpty()){
                rolesSep = userRequest.getRoles().stream().
                    map(String::valueOf).
                    collect(Collectors.joining(","));
            }
            return userManagementCustRepo.createUserManagement(userRequest.getUser(), userRequest.getPassword(), rolesSep);
        } catch (Exception e) {
            return new SpResponseDTO("Error al crear al usuarios: " + e.getMessage(),false);
        }  
    }

    @Override
    @Transactional
    public SpResponseDTO updateUserManagement(UserRolesUpdateManagementResponseDTO userRequest){
        try {
            String jsonUser = objectMapper.writeValueAsString(userRequest);
            System.out.println("JSON ENVIADO AL SP: " + jsonUser);
            return userManagementCustRepo.updateUserManagement(jsonUser);

        } catch (Exception e) {
            e.printStackTrace();
            return new SpResponseDTO("Error inesperado en el JSON: " + e.getMessage(), false);
        } 
    }

    @Override
    @Transactional(readOnly = true)
    public UserRoleManagementResponseDTO DataUserById(Integer idUser) {
        try {
            UserRoleManagementResponseDTO userManagement = userManagementCustRepo.DataUserById(idUser);

            if (userManagement.getRolesasignadosgu() != null && !userManagement.getRolesasignadosgu().isEmpty()) {

                List<Integer> rolesList = Arrays.stream(userManagement.getRolesasignadosgu().split(","))
                        .map(Integer::parseInt)
                        .toList();

                userManagement.setRoles(rolesList);
            }

            return userManagement;

        } catch (Exception e) {
            throw new RuntimeException("Error al obtener el usuario: " + e.getMessage());
        }
    }
}
