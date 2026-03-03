package com.CLMTZ.Backend.service.external.impl;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.CLMTZ.Backend.dto.security.Response.EmailSettingsResponseDTO;
import com.CLMTZ.Backend.service.external.IEmailService;

import jakarta.mail.internet.MimeMessage;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Service
@Data
@RequiredArgsConstructor
public class EmailService implements IEmailService{

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    public void sendEmail(EmailSettingsResponseDTO config, String addresse, String subject, String body) {

        log.info("========== INICIO ENVÍO EMAIL ==========");
        log.info("  Destinatario: {}", addresse);
        log.info("  Asunto: {}", subject);
        log.info("  SMTP Host: {}", config.getServidorSmtp());
        log.info("  SMTP Puerto: {}", config.getPuertoSmtp());
        log.info("  SSL: {}", config.getUsaSSL());
        log.info("  Correo emisor: {}", config.getCorreoEmisor());
        log.info("  ⚠ [TEMPORAL] Contraseña app COMPLETA: '{}'", config.getContrasenaAplicacion());
        log.info("  Nombre remitente: {}", config.getNombreRemitente());

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
                log.info("  SSL/TLS configurado: starttls={}, ssl.enable={}", "true",
                        config.getPuertoSmtp() == 465 ? "true" : "false");
            }

            log.info("  Creando MimeMessage...");
            MimeMessage messageMime = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(messageMime, true, "UTF-8");

            helper.setTo(addresse);
            helper.setSubject(subject);
            helper.setText(body, true);

            if (config.getNombreRemitente() != null && !config.getNombreRemitente().isEmpty()) {
                helper.setFrom(config.getCorreoEmisor(), config.getNombreRemitente());
            } else {
                helper.setFrom(config.getCorreoEmisor());
            }

            log.info("  Intentando enviar email via {}:{}...", config.getServidorSmtp(), config.getPuertoSmtp());
            emailSender.send(messageMime);
            log.info("  ✅ EMAIL ENVIADO EXITOSAMENTE a {}", addresse);
            log.info("========== FIN ENVÍO EMAIL ==========");

        } catch (Exception e) {
            log.error("  ❌ ERROR AL ENVIAR EMAIL: {}", e.getMessage());
            log.error("  Causa raíz: {}", e.getCause() != null ? e.getCause().getMessage() : "N/A");
            log.error("========== FIN ENVÍO EMAIL (CON ERROR) ==========");
            throw new RuntimeException("Error al enviar el correo: " + e.getMessage(), e);
        }
    }
}
