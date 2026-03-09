package com.CLMTZ.Backend.repository.security.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.CLMTZ.Backend.model.security.AccessAudit;

@Repository
public interface IAccessAuditRepository extends JpaRepository<AccessAudit, Integer>{

    // @Procedure(procedureName = "seguridad.sp_in_auditoriaacceso")
    // void createAccessAudit(@Param("p_usuariointentado") String attemptedUser, @Param("p_direccionip") String ipAddress,@Param("p_navegador") String browser, @Param("p_accion") String action);

    // @Procedure(procedureName = "seguridad.sp_in_auditoriacierresesion")
    // void createLogoutAudit(@Param("p_idusuario") Integer userId, @Param("p_direccionip") String ipAddress,@Param("p_navegador") String browser, @Param("p_accion") String action);

}