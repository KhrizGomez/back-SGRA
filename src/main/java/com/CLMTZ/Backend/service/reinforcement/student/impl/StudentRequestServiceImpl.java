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

        return new StudentRequestCreateResponseDTO(requestId);
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