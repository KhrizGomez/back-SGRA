package com.CLMTZ.Backend.service.general.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.CLMTZ.Backend.model.general.Notification;
import com.CLMTZ.Backend.model.general.Preference;
import com.CLMTZ.Backend.model.general.User;
import com.CLMTZ.Backend.model.reinforcement.Participants;
import com.CLMTZ.Backend.model.reinforcement.ReinforcementRequest;
import com.CLMTZ.Backend.model.reinforcement.ScheduledReinforcement;
import com.CLMTZ.Backend.model.reinforcement.ScheduledReinforcementDetail;
import com.CLMTZ.Backend.repository.general.INotificationRepository;
import com.CLMTZ.Backend.repository.general.IPreferenceRepository;
import com.CLMTZ.Backend.repository.reinforcement.jpa.IScheduledReinforcementRepository;
import com.CLMTZ.Backend.service.external.IEmailService;

import lombok.RequiredArgsConstructor;

/**
 * Scheduler que revisa cada minuto las sesiones próximas y envía recordatorios
 * a docentes y estudiantes según la anticipación configurada en sus preferencias.
 */
@Service
@RequiredArgsConstructor
public class ReminderSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(ReminderSchedulerService.class);
    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final IScheduledReinforcementRepository sessionRepo;
    private final IPreferenceRepository preferenceRepo;
    private final INotificationRepository notificationRepo;
    private final IEmailService emailService;

    @Scheduled(fixedRateString = "${sgra.reminder.check-interval-ms:60000}")
    @Transactional
    public void checkAndSendReminders() {
        //log.debug("Revisando recordatorios pendientes...");

        List<Integer> ids = sessionRepo.findIdsSesionesFuturas();
        if (ids.isEmpty()) return;
        List<ScheduledReinforcement> sessions = sessionRepo.findAllById(ids);
        LocalDateTime now = LocalDateTime.now();

        for (ScheduledReinforcement session : sessions) {
            LocalDate date = session.getScheduledDateReinforcement();
            LocalTime startTime = session.getTimeSlotId().getStartTime();
            LocalDateTime sessionDateTime = LocalDateTime.of(date, startTime);

            // Sesión ya pasó
            if (now.isAfter(sessionDateTime)) continue;

            List<User> usuarios = resolveUsuarios(session);

            for (User user : usuarios) {
                procesarRecordatorioUsuario(user, session, sessionDateTime, now);
            }
        }
    }

    // ─── Reúne docente + estudiantes participantes de la sesión ───

    private List<User> resolveUsuarios(ScheduledReinforcement session) {
        List<User> usuarios = new ArrayList<>();

        for (ScheduledReinforcementDetail detail : session.getScheduledReinforcementDetails()) {
            ReinforcementRequest request = detail.getReinforcementRequestId();

            // Docente
            if (request.getTeacherId() != null && request.getTeacherId().getUserId() != null) {
                User docente = request.getTeacherId().getUserId();
                if (!usuarios.contains(docente)) {
                    usuarios.add(docente);
                }
            }

            // Estudiantes participantes
            if (request.getParticipants() != null) {
                for (Participants p : request.getParticipants()) {
                    if (p.getStudentId() != null && p.getStudentId().getUserId() != null) {
                        User estudiante = p.getStudentId().getUserId();
                        if (!usuarios.contains(estudiante)) {
                            usuarios.add(estudiante);
                        }
                    }
                }
            }
        }

        return usuarios;
    }

    // ─── Evalúa si corresponde enviar recordatorio a un usuario ───

    private void procesarRecordatorioUsuario(User user, ScheduledReinforcement session,
                                              LocalDateTime sessionDateTime, LocalDateTime now) {
        Integer userId = user.getUserId();
        Integer sessionId = session.getScheduledReinforcementId();

        // Buscar preferencia del usuario
        Preference preference = preferenceRepo.findByUserId_UserId(userId).orElse(null);

        if (preference == null || preference.getReminderAdvance() == null
                || preference.getReminderAdvance() <= 0) {
            return; // Sin preferencia configurada → no enviar
        }

        LocalDateTime windowStart = sessionDateTime.minusMinutes(preference.getReminderAdvance());

        // ¿Estamos dentro de la ventana de recordatorio?
        if (now.isBefore(windowStart)) return;

        // ¿Ya se envió este recordatorio?
        boolean yaEnviado = notificationRepo
                .existsByUserId_UserIdAndScheduledReinforcement_ScheduledReinforcementId(userId, sessionId);

        if (yaEnviado) return;

        // Determinar canal de notificación
        String canal = preference.getNotificationChannelId() != null
                ? preference.getNotificationChannelId().getNameChannel().toUpperCase()
                : "EMAIL";

        enviarRecordatorio(user, session, sessionDateTime, canal);
        guardarNotificacion(user, session, sessionDateTime);
    }

    // ─── Envío según canal configurado ───

    private void enviarRecordatorio(User user, ScheduledReinforcement session,
                                    LocalDateTime sessionDateTime, String canal) {
        String asunto = "Recordatorio: Sesión de refuerzo el " + sessionDateTime.format(FECHA_FMT);
        String cuerpo = buildEmailBody(user, session, sessionDateTime);

        if (canal.contains("EMAIL") || canal.contains("CORREO")) {
            if (user.getEmail() != null && !user.getEmail().isBlank()) {
                try {
                    emailService.sendEmail(user.getEmail(), asunto, cuerpo);
                    log.info("Recordatorio enviado a {} (sesión #{}) via email",
                            user.getEmail(), session.getScheduledReinforcementId());
                } catch (Exception e) {
                    log.error("Error al enviar recordatorio a {} para sesión #{}: {}",
                            user.getEmail(), session.getScheduledReinforcementId(), e.getMessage());
                }
            } else {
                log.warn("Usuario #{} no tiene email configurado, recordatorio omitido", user.getUserId());
            }
        } else {
            log.info("Canal '{}' no implementado aún para usuario #{}, sesión #{}",
                    canal, user.getUserId(), session.getScheduledReinforcementId());
        }
    }

    // ─── Persiste la notificación para evitar duplicados ───

    private void guardarNotificacion(User user, ScheduledReinforcement session,
                                     LocalDateTime sessionDateTime) {
        Notification notif = new Notification();
        notif.setUserId(user);
        notif.setScheduledReinforcement(session);
        notif.setTitle("Recordatorio: Sesión de refuerzo - " + sessionDateTime.format(FECHA_FMT));
        notif.setMessage("Tienes una sesión de refuerzo programada para el "
                + sessionDateTime.format(FECHA_FMT) + " a las "
                + sessionDateTime.format(HORA_FMT) + ".");
        notif.setDateSent(LocalDateTime.now());
        notificationRepo.save(notif);
    }

    // ─── Cuerpo HTML del correo ───

    private String buildEmailBody(User user, ScheduledReinforcement session,
                                  LocalDateTime sessionDateTime) {
        String nombre = user.getFirstName() + " " + user.getLastName();
        String fecha = sessionDateTime.format(FECHA_FMT);
        String hora = sessionDateTime.format(HORA_FMT);
        String tipo = session.getSessionTypeId() != null
                ? session.getSessionTypeId().getSesionType() : "Refuerzo";
        String motivo = session.getReason() != null ? session.getReason() : "No especificado";

        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="margin:0; padding:0; background:#F4F7F5; font-family:'Segoe UI',Roboto,Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#F4F7F5; padding:32px 16px;">
                    <tr><td align="center">
                      <table width="580" cellpadding="0" cellspacing="0" style="background:#ffffff; border-radius:16px; overflow:hidden; box-shadow:0 4px 24px rgba(0,0,0,0.08);">

                        <!-- HEADER -->
                        <tr><td style="background:linear-gradient(135deg, #0D4F32 0%%, #0F5B3B 50%%, #116A43 100%%); padding:32px 24px; text-align:center;">
                          <h1 style="color:#ffffff; margin:0 0 4px; font-size:22px; font-weight:700; letter-spacing:-0.3px;">SGRA</h1>
                          <p style="color:rgba(255,255,255,0.75); margin:0; font-size:12px; font-weight:400; letter-spacing:0.5px; text-transform:uppercase;">Sistema de Gestión de Refuerzos Académicos</p>
                          <p style="color:rgba(255,255,255,0.6); margin:6px 0 0; font-size:11px;">Universidad Técnica Estatal de Quevedo</p>
                        </td></tr>

                        <!-- BODY -->
                        <tr><td style="padding:32px 28px 24px;">
                          <p style="color:#1a1a2e; font-size:16px; margin:0 0 8px; font-weight:600;">¡Hola, %s!</p>
                          <p style="color:#555770; font-size:14px; line-height:1.6; margin:0 0 24px;">
                            Te recordamos que tienes una <strong>sesión de refuerzo académico</strong> próximamente. A continuación los detalles:
                          </p>

                          <!-- DETALLES DE LA SESIÓN -->
                          <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f8faf9; border:1px solid #e0e8e4; border-radius:12px; overflow:hidden; margin:0 0 24px;">
                            <tr><td style="padding:16px 20px; border-bottom:1px solid #e0e8e4;">
                              <table width="100%%" cellpadding="0" cellspacing="0"><tr>
                                <td style="color:#6c757d; font-size:13px; font-weight:500; width:140px;">📚 Tipo de sesión</td>
                                <td style="color:#0D4F32; font-size:14px; font-weight:700;">%s</td>
                              </tr></table>
                            </td></tr>
                            <tr><td style="padding:16px 20px; border-bottom:1px solid #e0e8e4;">
                              <table width="100%%" cellpadding="0" cellspacing="0"><tr>
                                <td style="color:#6c757d; font-size:13px; font-weight:500; width:140px;">📅 Fecha</td>
                                <td style="color:#0D4F32; font-size:14px; font-weight:700; font-family:'Courier New',monospace;">%s</td>
                              </tr></table>
                            </td></tr>
                            <tr><td style="padding:16px 20px; border-bottom:1px solid #e0e8e4;">
                              <table width="100%%" cellpadding="0" cellspacing="0"><tr>
                                <td style="color:#6c757d; font-size:13px; font-weight:500; width:140px;">🕐 Hora de inicio</td>
                                <td style="color:#0D4F32; font-size:14px; font-weight:700; font-family:'Courier New',monospace;">%s</td>
                              </tr></table>
                            </td></tr>
                            <tr><td style="padding:16px 20px;">
                              <table width="100%%" cellpadding="0" cellspacing="0"><tr>
                                <td style="color:#6c757d; font-size:13px; font-weight:500; width:140px;">📝 Motivo</td>
                                <td style="color:#333; font-size:14px;">%s</td>
                              </tr></table>
                            </td></tr>
                          </table>

                          <!-- AVISO -->
                          <table width="100%%" cellpadding="0" cellspacing="0" style="background:#E8F5E9; border-left:4px solid #0D4F32; border-radius:0 8px 8px 0; margin:0 0 24px;">
                            <tr><td style="padding:14px 16px;">
                              <p style="color:#1B5E20; font-size:13px; margin:0; line-height:1.5;">
                                <strong>✅ Recuerda:</strong> Ingresa al sistema SGRA para revisar los recursos y detalles adicionales de tu sesión.
                              </p>
                            </td></tr>
                          </table>

                          <p style="color:#8b8da3; font-size:12px; margin:0; line-height:1.5; text-align:center;">
                            Este es un correo automático del SGRA. No responda a este mensaje.
                          </p>
                        </td></tr>

                        <!-- FOOTER -->
                        <tr><td style="background:#f8faf9; padding:16px 24px; text-align:center; border-top:1px solid #e8ece9;">
                          <p style="color:#8b8da3; font-size:11px; margin:0;">© 2026 UTEQ — Todos los derechos reservados</p>
                        </td></tr>

                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(nombre, tipo, fecha, hora, motivo);
    }
}
