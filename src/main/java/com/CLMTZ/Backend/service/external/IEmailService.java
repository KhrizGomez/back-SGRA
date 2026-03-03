package com.CLMTZ.Backend.service.external;

import com.CLMTZ.Backend.dto.security.Response.EmailSettingsResponseDTO;

public interface IEmailService {

    /**
     * Envía un email usando la configuración de correo activa del sistema.
     * Obtiene automáticamente la config de la BD y desencripta la contraseña.
     * Este es el método que cualquier módulo debería usar.
     *
     * @param to      correo destinatario
     * @param subject asunto del correo
     * @param body    cuerpo HTML del correo
     * @throws RuntimeException si no hay configuración activa o si falla el envío
     */
    void sendEmail(String to, String subject, String body);

    /**
     * Envía un email usando una configuración de correo específica (ya desencriptada).
     * Usar solo cuando se necesite enviar con una config diferente a la activa.
     */
    void sendEmail(EmailSettingsResponseDTO config, String to, String subject, String body);

}
