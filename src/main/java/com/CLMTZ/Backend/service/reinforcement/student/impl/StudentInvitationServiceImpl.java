package com.CLMTZ.Backend.service.reinforcement.student.impl;

import com.CLMTZ.Backend.config.UserContextHolder;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentInvitationHistoryDTO;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentInvitationItemDTO;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentInvitationResponseDTO;
import com.CLMTZ.Backend.repository.reinforcement.student.StudentInvitationRepository;
import com.CLMTZ.Backend.service.external.IEmailService;
import com.CLMTZ.Backend.service.reinforcement.student.StudentInvitationService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentInvitationServiceImpl implements StudentInvitationService {

    private final StudentInvitationRepository studentInvitationRepository;
    private final IEmailService emailService;

    public StudentInvitationServiceImpl(StudentInvitationRepository studentInvitationRepository,
                                       IEmailService emailService) {
        this.studentInvitationRepository = studentInvitationRepository;
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
        Integer userId = UserContextHolder.getContext().getUserId();
        // Obtener la invitación antes de procesar la respuesta para usar los datos en el correo
        StudentInvitationItemDTO invitation = studentInvitationRepository.listPendingInvitations(userId).stream()
                .filter(inv -> inv.getParticipantId().equals(participantId))
                .findFirst()
                .orElse(null);

        Boolean success = studentInvitationRepository.respondInvitation(userId, participantId, accept);

        if (success) {
            String message = accept ? "Has aceptado la invitación a la tutoría grupal"
                                    : "Has rechazado la invitación a la tutoría grupal";
            notifyRequester(invitation, accept);
            return new StudentInvitationResponseDTO(true, message);
        } else {
            return new StudentInvitationResponseDTO(false,
                    "No se pudo procesar la invitación. Verifica que aún esté pendiente.");
        }
    }

    private void notifyRequester(StudentInvitationItemDTO invitation, Boolean accept) {
        if (invitation == null || invitation.getRequesterEmail() == null || invitation.getRequesterEmail().isBlank()) {
            return;
        }

        String subject = "SGRA - Respuesta a invitación grupal (#" + invitation.getRequestId() + ")";
        String decision = Boolean.TRUE.equals(accept) ? "aceptó" : "rechazó";
        String body = buildCardEmail(
                "Invitación grupal",
                "Solicitud #" + invitation.getRequestId(),
                """
                        <p style=\"margin:0 0 12px;color:#0f172a;font-weight:600;\">%s %s la invitación a la sesión grupal.</p>
                        <div style=\"background:#f1f5f9;border:1px solid #e2e8f0;border-radius:10px;padding:14px 16px;margin-bottom:12px;\">
                          <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Asignatura:</strong> %s</div>
                          <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Solicitante:</strong> %s</div>
                        </div>
                        <p style=\"margin:12px 0 0;color:#64748b;font-size:12px;\">Ingresa a SGRA para gestionar la solicitud.</p>
                        """.formatted(invitation.getRequesterName(), decision, invitation.getSubjectName(), invitation.getRequesterName())
        );

        emailService.sendEmailAsync(invitation.getRequesterEmail(), subject, body);
    }

    private String buildCardEmail(String title, String subtitle, String bodyHtml) {
        return """
                <div style='font-family:Arial,sans-serif;max-width:640px;margin:0 auto;padding:18px;background:#eef2f7;'>
                  <div style='background:linear-gradient(135deg,#1B7505,#145904);color:#fff;padding:20px 22px;border-radius:14px 14px 0 0;'>
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
