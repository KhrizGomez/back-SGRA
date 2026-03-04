package com.CLMTZ.Backend.service.external;

import com.CLMTZ.Backend.dto.security.Response.EmailSettingsResponseDTO;

public interface IEmailService {

    /**
     * Envía un email usando la configuración de correo activa del sistema.
     */
    void sendEmail(String to, String subject, String body);

    /**
     * Envía un email usando una configuración de correo específica (ya desencriptada).
     */
    void sendEmail(EmailSettingsResponseDTO config, String to, String subject, String body);

}
