package com.CLMTZ.Backend.service.security;

import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;

public interface IPasswordRecoveryService {

    /**
     * Solicita un código de recuperación. Busca al usuario por email,
     * genera un código de 6 dígitos, lo almacena temporalmente y lo envía por correo.
     */
    SpResponseDTO requestRecoveryCode(String email);

    /**
     * Verifica que el código de recuperación sea válido y no haya expirado.
     */
    SpResponseDTO verifyRecoveryCode(String email, String code);

    /**
     * Resetea la contraseña del usuario tras verificar el código de recuperación.
     * Valida el código, valida la nueva contraseña y llama al SP de recuperación.
     */
    SpResponseDTO resetPassword(String email, String code, String newPassword, String confirmPassword);
}

