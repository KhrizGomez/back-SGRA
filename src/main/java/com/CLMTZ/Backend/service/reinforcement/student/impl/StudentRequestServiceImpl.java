package com.CLMTZ.Backend.service.reinforcement.student.impl;

import com.CLMTZ.Backend.dto.reinforcement.student.*;
import com.CLMTZ.Backend.repository.reinforcement.student.StudentRequestRepository;
import com.CLMTZ.Backend.service.external.IEmailService;
import com.CLMTZ.Backend.service.external.IStorageService;
import com.CLMTZ.Backend.service.reinforcement.student.StudentCatalogService;
import com.CLMTZ.Backend.service.reinforcement.student.StudentRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StudentRequestServiceImpl implements StudentRequestService {

    private static final Logger log = LoggerFactory.getLogger(StudentRequestServiceImpl.class);

    private final StudentRequestRepository studentRequestRepository;
    private final StudentCatalogService studentCatalogService;
    private final IStorageService storageService;
    private final IEmailService emailService;

    public StudentRequestServiceImpl(StudentRequestRepository studentRequestRepository,
                                     StudentCatalogService studentCatalogService,
                                     IStorageService storageService,
                                     IEmailService emailService) {
        this.studentRequestRepository = studentRequestRepository;
        this.studentCatalogService = studentCatalogService;
        this.storageService = storageService;
        this.emailService = emailService;
    }

    @Override
    public StudentRequestCreateResponseDTO create(StudentRequestCreateRequestDTO req, Integer userId, MultipartFile[] files) {
        // 1. Resolver el docente automáticamente por el paralelo del estudiante
        StudentSubjectTeacherDTO teacher = studentCatalogService.getTeacherForSubject(req.getSubjectId());
        if (teacher == null) {
            throw new IllegalStateException(
                    "No se encontró un docente asignado para esta asignatura en tu paralelo. " +
                    "Contacta a coordinación."
            );
        }

        // 2. Resolver el periodo activo
        ActivePeriodDTO activePeriod = studentCatalogService.getActivePeriod();
        if (activePeriod == null) {
            throw new IllegalStateException(
                    "No hay un periodo académico activo en este momento. Contacta a coordinación."
            );
        }

        // 3. Validar que sesiones grupales tengan al menos 1 participante
        boolean isGroupSession = req.getSessionTypeId() != null && req.getSessionTypeId() == 2;
        if (isGroupSession) {
            if (req.getParticipantIds() == null || req.getParticipantIds().isEmpty()) {
                throw new IllegalArgumentException(
                        "Para una sesión grupal debes seleccionar al menos 1 compañero."
                );
            }
        }

        // 4. Crear la solicitud en la base de datos
        Integer requestId = studentRequestRepository.createRequest(
                userId,
                req.getSubjectId(),
                teacher.getTeacherId(),
                req.getSessionTypeId(),
                req.getReason(),
                activePeriod.getPeriodId()
        );

        // 5. Subir archivos a Azure Blob Storage y registrar URLs
        if (files != null) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    String fileUrl = storageService.uploadFiles(file);
                    studentCatalogService.addResourceUrl(requestId, fileUrl);
                }
            }
        }

        // 6. Si es sesión grupal, insertar participantes y notificar por correo
        if (isGroupSession && req.getParticipantIds() != null && !req.getParticipantIds().isEmpty()) {
            studentRequestRepository.addParticipants(requestId, req.getParticipantIds());

            // Obtener datos de los compañeros para enviar correos
            sendGroupSessionEmails(req.getSubjectId(), req.getParticipantIds(), requestId);
        }

        // 7. Notificar creación de solicitud (estudiante solicitante y docente asignado)
        sendCreationEmails(requestId, teacher);

        return new StudentRequestCreateResponseDTO(requestId);
    }

    private void sendCreationEmails(Integer requestId, StudentSubjectTeacherDTO teacher) {
        try {
            var summary = studentRequestRepository.getRequestSummary(requestId);
            if (summary == null) {
                log.warn("No se encontró resumen para solicitud #{}, se omite email de creación", requestId);
                return;
            }

            String subjectLine = "SGRA - Nueva solicitud de refuerzo (#" + requestId + ")";

            // Email al estudiante solicitante
            if (summary.getStudentEmail() != null && !summary.getStudentEmail().isBlank()) {
                String bodyStudent = buildNewRequestEmailForStudent(summary);
                emailService.sendEmailAsync(summary.getStudentEmail(), subjectLine, bodyStudent);
            }

            // Email al docente asignado
            String teacherEmail = teacher != null ? teacher.getEmail() : summary.getTeacherEmail();
            if (teacherEmail != null && !teacherEmail.isBlank()) {
                String bodyTeacher = buildNewRequestEmailForTeacher(summary);
                emailService.sendEmailAsync(teacherEmail, subjectLine, bodyTeacher);
            }
        } catch (Exception ex) {
            log.error("No se pudo enviar emails de creación para solicitud #{}: {}", requestId, ex.getMessage());
        }
    }

    private String buildNewRequestEmailForStudent(StudentRequestSummaryDTO summary) {
        return buildSgraCard(
                "SGRA - Solicitud registrada",
                "Hola %s".formatted(summary.getStudentName()),
                """
                        <p style=\"margin:0 0 14px;color:#1f2937;font-weight:600;\">Tu solicitud de refuerzo se registró con éxito.</p>
                        <div style=\"background:#f1f5f9;border:1px solid #e2e8f0;border-radius:10px;padding:14px 16px;margin-bottom:12px;\">
                          <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Solicitud:</strong> #%d</div>
                          <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Asignatura:</strong> %s</div>
                          <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Curso:</strong> %s</div>
                          <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Paralelo:</strong> %s</div>
                          <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Docente asignado:</strong> %s</div>
                          <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Motivo:</strong> %s</div>
                        </div>
                        <p style=\"margin:12px 0 0;color:#64748b;font-size:12px;\">Este es un correo automático del Sistema de Gestión de Refuerzos Académicos.</p>
                        """.formatted(
                        summary.getRequestId(),
                        summary.getSubjectName(),
                        summary.getCourseName(),
                        summary.getParallelName(),
                        summary.getTeacherName(),
                        summary.getReason() != null ? summary.getReason() : "(sin motivo)"
                )
        );
    }

    private String buildNewRequestEmailForTeacher(StudentRequestSummaryDTO summary) {
        return buildSgraCard(
                "SGRA - Nueva solicitud recibida",
                "Docente %s".formatted(summary.getTeacherName()),
                """
                        <p style=\"margin:0 0 14px;color:#1f2937;font-weight:600;\">Recibiste una nueva solicitud de refuerzo.</p>
                        <div style=\"background:#f1f5f9;border:1px solid #e2e8f0;border-radius:10px;padding:14px 16px;margin-bottom:14px;\">
                          <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Solicitud:</strong> #%d</div>
                          <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Estudiante:</strong> %s</div>
                          <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Asignatura:</strong> %s</div>
                          <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Curso:</strong> %s</div>
                          <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Paralelo:</strong> %s</div>
                          <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Motivo:</strong> %s</div>
                        </div>
                        <p style=\"margin:12px 0 0;color:#64748b;font-size:12px;\">Ingresa a SGRA para aceptar o programar la sesión.</p>
                        """.formatted(
                        summary.getRequestId(),
                        summary.getStudentName(),
                        summary.getSubjectName(),
                        summary.getCourseName(),
                        summary.getParallelName(),
                        summary.getReason() != null ? summary.getReason() : "(sin motivo)"
                )
        );
    }

    private String buildSgraCard(String title, String subtitle, String bodyHtml) {
        return """
                <div style='font-family:Arial,sans-serif;max-width:640px;margin:0 auto;padding:18px;background:#eef2f7;'>
                  <div style='background:linear-gradient(135deg,#0D4F32,#0F5B3B);color:#fff;padding:20px 22px;border-radius:14px 14px 0 0;'>
                    <h2 style='margin:0;font-size:21px;font-weight:800;'>%s</h2>
                    <p style='margin:6px 0 0;font-size:13px;color:rgba(255,255,255,0.9);'>%s</p>
                  </div>
                  <div style='border:1px solid #e2e8f0;border-top:none;border-radius:0 0 14px 14px;padding:22px 22px;background:#fff;'>
                    %s
                  </div>
                </div>
                """.formatted(title, subtitle, bodyHtml);
    }

    /**
     * Envía correos de notificación a los participantes de una solicitud grupal.
     * El envío es asíncrono (best-effort): si falla un correo, se loguea el error
     * pero no se revierte la solicitud.
     */
    private void sendGroupSessionEmails(Integer subjectId, List<Integer> participantIds, Integer requestId) {
        try {
            // Obtener la lista de compañeros con sus emails
            List<ClassmateItemDTO> allClassmates = studentCatalogService.getClassmatesBySubject(subjectId);

            // Filtrar solo los seleccionados como participantes
            Set<Integer> participantIdSet = Set.copyOf(participantIds);
            List<ClassmateItemDTO> selectedParticipants = allClassmates.stream()
                    .filter(c -> participantIdSet.contains(c.getStudentId()))
                    .collect(Collectors.toList());

            // Obtener nombre de la asignatura para el correo
            List<SubjectItemDTO> subjects = studentCatalogService.getEnrolledSubjects();
            String subjectName = subjects.stream()
                    .filter(s -> s.getSubjectId().equals(subjectId))
                    .map(SubjectItemDTO::getSubjectName)
                    .findFirst()
                    .orElse("Asignatura");

            for (ClassmateItemDTO participant : selectedParticipants) {
                try {
                    String subject = "SGRA - Has sido invitado a una tutoría grupal";
                    String body = buildGroupInvitationEmail(
                            participant.getFullName(),
                            subjectName,
                            requestId
                    );
                    emailService.sendEmail(participant.getEmail(), subject, body);
                    log.info("📧 Correo de invitación grupal enviado a: {} (solicitud #{})",
                            participant.getEmail(), requestId);
                } catch (Exception emailEx) {
                    log.error("❌ Error al enviar correo de invitación a {}: {}",
                            participant.getEmail(), emailEx.getMessage());
                    // No lanzar excepción: el correo es best-effort
                }
            }
        } catch (Exception ex) {
            log.error("❌ Error general al enviar correos de invitación grupal (solicitud #{}): {}",
                    requestId, ex.getMessage());
        }
    }

    /**
     * Construye el cuerpo HTML del correo de invitación a tutoría grupal.
     */
    private String buildGroupInvitationEmail(String participantName, String subjectName, Integer requestId) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="background: linear-gradient(135deg, #198754, #157347); padding: 30px; border-radius: 10px 10px 0 0; text-align: center;">
                        <h1 style="color: white; margin: 0; font-size: 24px;">📚 SGRA - Tutoría Grupal</h1>
                    </div>
                    <div style="background: #ffffff; padding: 30px; border: 1px solid #e0e0e0; border-top: none; border-radius: 0 0 10px 10px;">
                        <h2 style="color: #333; margin-top: 0;">¡Hola, %s!</h2>
                        <p style="color: #555; font-size: 16px; line-height: 1.6;">
                            Has sido invitado/a a participar en una <strong>sesión de tutoría grupal</strong>
                            para la asignatura:
                        </p>
                        <div style="background: #f8f9fa; border-left: 4px solid #198754; padding: 15px; margin: 20px 0; border-radius: 4px;">
                            <p style="margin: 0; font-size: 18px; color: #198754; font-weight: bold;">%s</p>
                            <p style="margin: 5px 0 0; color: #777;">Solicitud #%d</p>
                        </div>
                        <p style="color: #555; font-size: 16px; line-height: 1.6;">
                            Revisa tus <strong>invitaciones pendientes</strong> en el sistema SGRA para
                            aceptar o rechazar la invitación.
                        </p>
                        <div style="text-align: center; margin: 30px 0;">
                            <p style="color: #999; font-size: 13px;">
                                Este es un correo automático del Sistema de Gestión de Refuerzo Académico (SGRA).
                                Por favor no responda a este mensaje.
                            </p>
                        </div>
                    </div>
                </div>
                """.formatted(participantName, subjectName, requestId);
    }
}
