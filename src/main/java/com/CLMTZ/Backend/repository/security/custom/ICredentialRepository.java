package com.CLMTZ.Backend.repository.security.custom;

import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;

public interface ICredentialRepository {

    /**
     * Llama a seguridad.sp_in_credenciales_nuevo_usuario para crear credenciales
     * de acceso APP (estado='C', contraseña=cédula hasheada con bcrypt).
     *
     * @param userId ID del usuario en general.tbusuarios
     * @param roleId Rol APP (3=Docente, 4=Estudiante)
     * @return SpResponseDTO con mensaje (incluye username generado) y éxito
     */
    SpResponseDTO createNewUserCredentials(Integer userId, Integer roleId);

    /**
     * Llama a seguridad.sp_up_primer_cambio_contrasena para procesar el primer
     * cambio de contraseña. Pipeline completo: actualiza tbaccesos (estado='A'),
     * crea usuario PostgreSQL SERVER, vincula en tbusuariosgestionusuarios.
     *
     * @param userId      ID del usuario en general.tbusuarios
     * @param newPassword Nueva contraseña en TEXTO PLANO (el SP la hashea)
     * @return SpResponseDTO con mensaje y éxito
     */
    SpResponseDTO firstPasswordChange(Integer userId, String newPassword);
}

