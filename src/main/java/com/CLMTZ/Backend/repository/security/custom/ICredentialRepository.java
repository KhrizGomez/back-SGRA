package com.CLMTZ.Backend.repository.security.custom;

import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;

public interface ICredentialRepository {

    /**
     * Llama a seguridad.sp_in_credenciales_nuevo_usuario para crear credenciales
     */
    SpResponseDTO createNewUserCredentials(Integer userId, Integer roleId);

    /**
     * Llama a seguridad.sp_up_primer_cambio_contrasena para procesar el primer
     * cambio de contraseña.
     */
    SpResponseDTO firstPasswordChange(Integer userId, String newPassword);

    /**
     * Llama a seguridad.sp_up_recuperar_contrasena para recuperar la contraseña
     * de un usuario existente (estado='A').
     */
    SpResponseDTO recoverPassword(Integer userId, String newPassword);

    /**
     * Llama a seguridad.sp_up_cambiar_contrasena para cambio voluntario de contraseña.
     * Requiere la contraseña actual para verificación.
     */
    SpResponseDTO voluntaryPasswordChange(Integer userId, String currentPassword, String newPassword);
}

