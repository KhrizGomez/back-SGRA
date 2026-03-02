package com.CLMTZ.Backend.repository.security.custom.impl;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;
import com.CLMTZ.Backend.repository.security.custom.IRoleManagementRoleCustomRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RoleManagementRoleCustomRepository implements IRoleManagementRoleCustomRepository{
    
    private final EntityManager entityManager;

    @Override
    @Transactional
    public SpResponseDTO updateRoleGRoleAssignment(String jsonAssignment){

        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("seguridad.sp_in_up_asignacionroles");

        query.registerStoredProcedureParameter("p_json_asignaciones", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_mensaje", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("p_exito", Boolean.class, ParameterMode.OUT);

        query.setParameter("p_json_asignaciones", jsonAssignment);

        query.execute();

        String message = (String) query.getOutputParameterValue("p_mensaje");
        Boolean success = (Boolean) query.getOutputParameterValue("p_exito");

        return new SpResponseDTO(message, success);
    }
}
