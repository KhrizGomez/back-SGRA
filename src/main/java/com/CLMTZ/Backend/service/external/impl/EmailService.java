package com.CLMTZ.Backend.service.external.impl;

import java.util.Properties;

import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.CLMTZ.Backend.service.external.IEmailService;

import jakarta.mail.internet.MimeMessage;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Service
@Data
@RequiredArgsConstructor
public class EmailService implements IEmailService{

    private String sender;
    private String subject;
    private String passwordApp;

    public void sendGmail(String addresse, String body) {

        try {

            JavaMailSenderImpl emailSender = new JavaMailSenderImpl();

            emailSender.setHost("smtp.gmail.com");
            emailSender.setPort(587);

            emailSender.setUsername(sender);
            emailSender.setPassword(passwordApp);

            Properties props = emailSender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");

            MimeMessage messageMime = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(messageMime, true);

            helper.setTo(addresse);
            helper.setSubject(subject);
            helper.setText(body); //helper.setText(body, true); Si deseo enviar un html
            helper.setFrom(sender);

            emailSender.send(messageMime);
        } catch (Exception e) {
            throw new RuntimeException("Error al enviar el correo: " + e.getMessage());
        }
    }
}
