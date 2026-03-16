package com.CLMTZ.Backend.service.external;

import com.CLMTZ.Backend.dto.security.Response.EmailSettingsResponseDTO;

public interface IEmailService {

    /**
     * Envía un email usando la configuración de correo activa del sistema.
     */
    void sendEmail(String to, String subject, String body);

    /**
     * Envía un email de forma asíncrona (no bloquea el hilo llamante).
     * Usar para cargas masivas donde no se requiere esperar confirmación.
     */
    void sendEmailAsync(String to, String subject, String body);

    boolean testSmtpConnection(EmailSettingsResponseDTO config);

}
