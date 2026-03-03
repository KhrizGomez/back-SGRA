package com.CLMTZ.Backend.service.external.impl;

import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.CLMTZ.Backend.dto.security.Response.EmailSettingsResponseDTO;
import com.CLMTZ.Backend.service.external.IEmailService;

import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

/**
 * Servicio general de envío de correos electrónicos.
 * Obtiene la configuración activa directamente de la BD vía función almacenada (sin sesión dinámica).
 * Cualquier módulo puede inyectar IEmailService y enviar correos con sendEmail(to, subject, body).
 */
@Service
@RequiredArgsConstructor
public class EmailService implements IEmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${sgra.master-key}")
    private String masterKey;

    // ─── Método simplificado: cualquier módulo puede usarlo ───

    @Override
    public void sendEmail(String to, String subject, String body) {
        EmailSettingsResponseDTO config = getActiveEmailConfig();
        sendEmail(config, to, subject, body);
    }

    // ─── Método con config explícita (mantiene compatibilidad) ───

    @Override
    public void sendEmail(EmailSettingsResponseDTO config, String to, String subject, String body) {
        try {
            JavaMailSenderImpl emailSender = new JavaMailSenderImpl();

            emailSender.setHost(config.getServidorSmtp());
            emailSender.setPort(config.getPuertoSmtp());
            emailSender.setUsername(config.getCorreoEmisor());
            emailSender.setPassword(config.getContrasenaAplicacion());

            Properties props = emailSender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true");

            if (config.getUsaSSL()) {
                props.put("mail.smtp.starttls.enable", "true");
                if (config.getPuertoSmtp() == 465) {
                    props.put("mail.smtp.ssl.enable", "true");
                }
            }

            MimeMessage messageMime = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(messageMime, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);

            if (config.getNombreRemitente() != null && !config.getNombreRemitente().isEmpty()) {
                helper.setFrom(config.getCorreoEmisor(), config.getNombreRemitente());
            } else {
                helper.setFrom(config.getCorreoEmisor());
            }

            emailSender.send(messageMime);
            log.info("✅ Email enviado exitosamente a: {}", to);

        } catch (Exception e) {
            log.error("❌ Error al enviar email a {}: {}", to, e.getMessage());
            throw new RuntimeException("Error al enviar el correo: " + e.getMessage(), e);
        }
    }

    // ─── Obtener config activa desencriptada desde BD (vía función almacenada) ───

    @SuppressWarnings("unchecked")
    private EmailSettingsResponseDTO getActiveEmailConfig() {
        if (masterKey == null || masterKey.isBlank()) {
            throw new RuntimeException("Master key no configurada en el sistema");
        }

        String sql = "SELECT * FROM seguridad.fn_get_config_correo_activa(?1)";

        List<Object[]> results = entityManager.createNativeQuery(sql)
                .setParameter(1, masterKey)
                .getResultList();

        if (results.isEmpty()) {
            throw new RuntimeException("No se encontró ninguna configuración de correo activa en la BD");
        }

        Object[] row = results.getFirst();
        EmailSettingsResponseDTO config = new EmailSettingsResponseDTO(
                (String) row[0],                         // servidorSmtp
                ((Number) row[1]).intValue(),             // puertoSmtp
                (Boolean) row[2],                        // usaSSL
                (String) row[3],                         // correoEmisor
                (String) row[4],                         // contrasenaAplicacion (desencriptada)
                (String) row[5]                          // nombreRemitente
        );

        return config;
    }
}
