package com.CLMTZ.Backend.repository.security.custom.impl;

import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;
import com.CLMTZ.Backend.repository.security.custom.ICredentialRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public class CredentialRepositoryImpl implements ICredentialRepository {

    private static final Logger log = LoggerFactory.getLogger(CredentialRepositoryImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public SpResponseDTO createNewUserCredentials(Integer userId, Integer roleId) {
        try {
            StoredProcedureQuery query = entityManager
                    .createStoredProcedureQuery("seguridad.sp_in_credenciales_nuevo_usuario");

            query.registerStoredProcedureParameter("p_idusuario", Integer.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_idrol", Integer.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_nombreusuario", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("p_mensaje", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("p_exito", Boolean.class, ParameterMode.OUT);

            query.setParameter("p_idusuario", userId);
            query.setParameter("p_idrol", roleId);
            query.execute();

            String username = (String) query.getOutputParameterValue("p_nombreusuario");
            String mensaje = (String) query.getOutputParameterValue("p_mensaje");
            Boolean exito = (Boolean) query.getOutputParameterValue("p_exito");

            log.info("sp_in_credenciales_nuevo_usuario → userId={}, roleId={}, username={}, exito={}, mensaje={}",
                    userId, roleId, username, exito, mensaje);

            return new SpResponseDTO(mensaje, exito);

        } catch (Exception e) {
            log.error("Error al llamar sp_in_credenciales_nuevo_usuario para userId={}: {}", userId, e.getMessage(), e);
            return new SpResponseDTO("Error interno al crear credenciales: " + e.getMessage(), false);
        }
    }

    @Override
    public SpResponseDTO firstPasswordChange(Integer userId, String newPassword) {
        try {
            StoredProcedureQuery query = entityManager
                    .createStoredProcedureQuery("seguridad.sp_up_primer_cambio_contrasena");

            query.registerStoredProcedureParameter("p_idusuario", Integer.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_nueva_contrasena", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_mensaje", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("p_exito", Boolean.class, ParameterMode.OUT);

            query.setParameter("p_idusuario", userId);
            query.setParameter("p_nueva_contrasena", newPassword);
            query.execute();

            String mensaje = (String) query.getOutputParameterValue("p_mensaje");
            Boolean exito = (Boolean) query.getOutputParameterValue("p_exito");

            log.info("sp_up_primer_cambio_contrasena → userId={}, exito={}, mensaje={}", userId, exito, mensaje);

            return new SpResponseDTO(mensaje, exito);

        } catch (Exception e) {
            log.error("Error al llamar sp_up_primer_cambio_contrasena para userId={}: {}", userId, e.getMessage(), e);
            return new SpResponseDTO("Error interno al cambiar contraseña: " + e.getMessage(), false);
        }
    }

    @Override
    public SpResponseDTO recoverPassword(Integer userId, String newPassword) {
        try {
            StoredProcedureQuery query = entityManager
                    .createStoredProcedureQuery("seguridad.sp_up_recuperar_contrasena");

            query.registerStoredProcedureParameter("p_idusuario", Integer.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_nueva_contrasena", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_mensaje", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("p_exito", Boolean.class, ParameterMode.OUT);

            query.setParameter("p_idusuario", userId);
            query.setParameter("p_nueva_contrasena", newPassword);
            query.execute();

            String mensaje = (String) query.getOutputParameterValue("p_mensaje");
            Boolean exito = (Boolean) query.getOutputParameterValue("p_exito");

            log.info("sp_up_recuperar_contrasena → userId={}, exito={}, mensaje={}", userId, exito, mensaje);

            return new SpResponseDTO(mensaje, exito);

        } catch (Exception e) {
            log.error("Error al llamar sp_up_recuperar_contrasena para userId={}: {}", userId, e.getMessage(), e);
            return new SpResponseDTO("Error interno al recuperar contraseña: " + e.getMessage(), false);
        }
    }
}

