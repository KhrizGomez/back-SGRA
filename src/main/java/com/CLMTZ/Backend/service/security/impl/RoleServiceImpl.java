package com.CLMTZ.Backend.service.security.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.CLMTZ.Backend.dto.security.Request.RoleRequestDTO;
import com.CLMTZ.Backend.model.security.Role;
import com.CLMTZ.Backend.repository.security.jpa.IRoleRepository;
import com.CLMTZ.Backend.service.security.IRoleService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements IRoleService {

    private final IRoleRepository roleRepo;

    @Override
    @Transactional(readOnly = true)
    public List<RoleRequestDTO> listRoleNames(){

        try {
            List<Role> listroles = roleRepo.findByState(true);

            return listroles.stream().map( listrol -> {
                RoleRequestDTO roleRequest = new RoleRequestDTO();
                roleRequest.setRoleId(listrol.getRoleId());
                roleRequest.setRole(listrol.getRole());
                return roleRequest;
            }).toList();

        } catch (Exception e) {
            throw new RuntimeException("Error al cargar el listado de roles del aplicativo: " + e.getMessage());
        }
        
    }
}
