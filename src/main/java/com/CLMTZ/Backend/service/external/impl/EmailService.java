package com.CLMTZ.Backend.service.external.impl;

import java.util.Properties;

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

    public void sendEmail(EmailSettingsResponseDTO config, String addresse, String subject, String body) {

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

            helper.setTo(addresse);
            helper.setSubject(subject);
            helper.setText(body, true);

            if (config.getNombreRemitente() != null && !config.getNombreRemitente().isEmpty()) {
                helper.setFrom(config.getCorreoEmisor(), config.getNombreRemitente());
            } else {
                helper.setFrom(config.getCorreoEmisor());
            }

            emailSender.send(messageMime);

        } catch (Exception e) {
            throw new RuntimeException("Error al enviar el correo: " + e.getMessage(), e);
        }
    }
}
