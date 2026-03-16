package com.CLMTZ.Backend.service.reinforcement.student.impl;

import com.CLMTZ.Backend.dto.reinforcement.student.StudentCancelRequestResponseDTO;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentRequestSummaryDTO;
import com.CLMTZ.Backend.repository.reinforcement.student.StudentRequestActionRepository;
import com.CLMTZ.Backend.repository.reinforcement.student.StudentRequestRepository;
import com.CLMTZ.Backend.service.external.IEmailService;
import com.CLMTZ.Backend.service.reinforcement.student.StudentRequestActionService;
import org.springframework.stereotype.Service;

@Service
public class StudentRequestActionServiceImpl implements StudentRequestActionService {

    private final StudentRequestActionRepository studentRequestActionRepository;
    private final StudentRequestRepository studentRequestRepository;
    private final IEmailService emailService;

    public StudentRequestActionServiceImpl(StudentRequestActionRepository studentRequestActionRepository,
                                           StudentRequestRepository studentRequestRepository,
                                           IEmailService emailService) {
        this.studentRequestActionRepository = studentRequestActionRepository;
        this.studentRequestRepository = studentRequestRepository;
        this.emailService = emailService;
    }

    @Override
    public StudentCancelRequestResponseDTO cancelRequest(Integer userId, Integer requestId) {
        StudentCancelRequestResponseDTO response = studentRequestActionRepository.cancelRequest(userId, requestId);

        if (response != null && "CANCELLED".equalsIgnoreCase(response.getStatus())) {
            sendCancelEmails(requestId);
        }

        return response;
    }

    private void sendCancelEmails(Integer requestId) {
        StudentRequestSummaryDTO summary = studentRequestRepository.getRequestSummary(requestId);
        if (summary == null) {
            return;
        }

        String subject = "SGRA - Solicitud cancelada (#" + requestId + ")";
        String reason = summary.getReason() != null ? summary.getReason() : "(sin motivo)";

        String studentBody = buildCardEmail(
                "Solicitud cancelada",
                "Tu solicitud fue cancelada",
                """
                        <div style=\"background:#f1f5f9;border:1px solid #e2e8f0;border-radius:10px;padding:14px 16px;margin-bottom:12px;\">
                          <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Solicitud:</strong> #%d</div>
                          <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Asignatura:</strong> %s</div>
                          <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Curso:</strong> %s</div>
                          <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Paralelo:</strong> %s</div>
                          <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Docente:</strong> %s</div>
                          <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Motivo original:</strong> %s</div>
                        </div>
                        <p style=\"margin:12px 0 0;color:#64748b;font-size:12px;\">Este es un aviso automático de SGRA.</p>
                        """.formatted(requestId, summary.getSubjectName(), summary.getCourseName(), summary.getParallelName(), summary.getTeacherName(), reason)
        );

        if (summary.getStudentEmail() != null && !summary.getStudentEmail().isBlank()) {
            emailService.sendEmailAsync(summary.getStudentEmail(), subject, studentBody);
        }

        if (summary.getTeacherEmail() != null && !summary.getTeacherEmail().isBlank()) {
            String teacherBody = buildCardEmail(
                    "Solicitud cancelada",
                    "Un estudiante canceló su solicitud",
                    """
                            <div style=\"background:#f1f5f9;border:1px solid #e2e8f0;border-radius:10px;padding:14px 16px;margin-bottom:12px;\">
                              <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Solicitud:</strong> #%d</div>
                              <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Estudiante:</strong> %s</div>
                              <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Asignatura:</strong> %s</div>
                              <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Curso:</strong> %s</div>
                              <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Paralelo:</strong> %s</div>
                              <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Motivo original:</strong> %s</div>
                            </div>
                            <p style=\"margin:12px 0 0;color:#64748b;font-size:12px;\">Ingresa a SGRA para ver el detalle.</p>
                            """.formatted(requestId, summary.getStudentName(), summary.getSubjectName(), summary.getCourseName(), summary.getParallelName(), reason)
            );
            emailService.sendEmailAsync(summary.getTeacherEmail(), subject, teacherBody);
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