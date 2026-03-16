package com.CLMTZ.Backend.service.reinforcement.student.impl;

import com.CLMTZ.Backend.config.UserContextHolder;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentInvitationHistoryDTO;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentInvitationItemDTO;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentInvitationResponseDTO;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentRequestSummaryDTO;
import com.CLMTZ.Backend.dto.security.session.UserContext;
import com.CLMTZ.Backend.repository.reinforcement.student.StudentInvitationRepository;
import com.CLMTZ.Backend.repository.reinforcement.student.StudentRequestRepository;
import com.CLMTZ.Backend.service.external.IEmailService;
import com.CLMTZ.Backend.service.reinforcement.student.StudentInvitationService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentInvitationServiceImpl implements StudentInvitationService {

    private final StudentInvitationRepository studentInvitationRepository;
    private final StudentRequestRepository studentRequestRepository;
    private final IEmailService emailService;

    public StudentInvitationServiceImpl(StudentInvitationRepository studentInvitationRepository,
                                        StudentRequestRepository studentRequestRepository,
                                        IEmailService emailService) {
        this.studentInvitationRepository = studentInvitationRepository;
        this.studentRequestRepository = studentRequestRepository;
        this.emailService = emailService;
    }

    @Override
    public List<StudentInvitationItemDTO> getPendingInvitations() {
        Integer userId = UserContextHolder.getContext().getUserId();
        return studentInvitationRepository.listPendingInvitations(userId);
    }

    @Override
    public List<StudentInvitationHistoryDTO> getInvitationHistory() {
        Integer userId = UserContextHolder.getContext().getUserId();
        return studentInvitationRepository.listInvitationHistory(userId);
    }

    @Override
    public StudentInvitationResponseDTO respondInvitation(Integer participantId, Boolean accept) {
        UserContext ctx = UserContextHolder.getContext();
        Integer userId = ctx.getUserId();

        // Capturar la invitación antes de responderla para obtener requestId y correos
        StudentInvitationItemDTO invitation = findInvitationForParticipant(userId, participantId);

        Boolean success = studentInvitationRepository.respondInvitation(userId, participantId, accept);

        if (Boolean.TRUE.equals(success)) {
            notifyInvitationResponse(ctx, invitation, accept);
            String message = accept ? "Has aceptado la invitación a la tutoría grupal"
                                    : "Has rechazado la invitación a la tutoría grupal";
            return new StudentInvitationResponseDTO(true, message);
        } else {
            return new StudentInvitationResponseDTO(false,
                    "No se pudo procesar la invitación. Verifica que aún esté pendiente.");
        }
    }

    private StudentInvitationItemDTO findInvitationForParticipant(Integer userId, Integer participantId) {
        return studentInvitationRepository.listPendingInvitations(userId).stream()
                .filter(inv -> inv.getParticipantId().equals(participantId))
                .findFirst()
                .orElse(null);
    }

    private void notifyInvitationResponse(UserContext ctx, StudentInvitationItemDTO invitation, boolean accept) {
        if (invitation == null) {
            return; // No tenemos contexto para notificar
        }

        StudentRequestSummaryDTO summary = studentRequestRepository.getRequestSummary(invitation.getRequestId());
        if (summary == null) {
            return;
        }

        String participantName = (ctx.getFirstName() != null ? ctx.getFirstName() : "") +
                (ctx.getLastName() != null ? " " + ctx.getLastName() : "");
        String statusText = accept ? "aceptó" : "rechazó";

        String subject = "SGRA - Invitación grupal " + (accept ? "aceptada" : "rechazada") +
                " (#" + invitation.getRequestId() + ")";

        String body = buildCardEmail(
                "Invitación grupal",
                "Participante " + statusText,
                """
                        <div style=\"background:#f1f5f9;border:1px solid #e2e8f0;border-radius:10px;padding:14px 16px;margin-bottom:12px;\">
                          <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Solicitud:</strong> #%d</div>
                          <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Asignatura:</strong> %s</div>
                          <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Curso:</strong> %s</div>
                          <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Paralelo:</strong> %s</div>
                          <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Docente:</strong> %s</div>
                          <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Participante:</strong> %s</div>
                          <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Acción:</strong> %s</div>
                        </div>
                        <p style=\"margin:12px 0 0;color:#64748b;font-size:12px;\">Aviso automático de SGRA.</p>
                        """.formatted(invitation.getRequestId(), summary.getSubjectName(), summary.getCourseName(), summary.getParallelName(), summary.getTeacherName(), participantName.trim(), accept ? "Aceptó la invitación" : "Rechazó la invitación")
        );

        // Notificar al docente (si tiene correo)
        if (summary.getTeacherEmail() != null && !summary.getTeacherEmail().isBlank()) {
            emailService.sendEmailAsync(summary.getTeacherEmail(), subject, body);
        }

        // Notificar al solicitante original (studentEmail en el summary)
        if (summary.getStudentEmail() != null && !summary.getStudentEmail().isBlank()) {
            emailService.sendEmailAsync(summary.getStudentEmail(), subject, body);
        }
    }

    private String buildCardEmail(String title, String subtitle, String bodyHtml) {
        return """
                <div style='font-family:Arial,sans-serif;max-width:640px;margin:0 auto;padding:18px;background:#eef2f7;'>
                  <div style='background:linear-gradient(135deg,#0D4F32,#0F5B3B);color:#fff;padding:20px 22px;border-radius:14px 14px 0 0;'>
                    <h2 style='margin:0;font-size:20px;font-weight:800;'>%s</h2>
                    <p style='margin:6px 0 0;font-size:13px;color:rgba(255,255,255,0.9);'>%s</p>
                  </div>
                  <div style='border:1px solid #e2e8f0;border-top:none;border-radius:0 0 14px 14px;padding:22px 22px;background:#fff;'>
                    %s
                  </div>
                </div>
                """.formatted(title, subtitle, bodyHtml);
    }
}
