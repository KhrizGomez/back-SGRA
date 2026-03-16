package com.CLMTZ.Backend.service.reinforcement.teacher.impl;

import com.CLMTZ.Backend.dto.reinforcement.student.StudentRequestSummaryDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherActionResponseDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherRequestsPageDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherScheduleRequestDTO;
import com.CLMTZ.Backend.model.academic.TimeSlot;
import com.CLMTZ.Backend.repository.academic.ITimeSlotRepository;
import com.CLMTZ.Backend.repository.reinforcement.teacher.TeacherRequestRepository;
import com.CLMTZ.Backend.service.external.IEmailService;
import com.CLMTZ.Backend.service.reinforcement.teacher.TeacherRequestService;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class TeacherRequestServiceImpl implements TeacherRequestService {

    private final TeacherRequestRepository teacherRequestRepository;
    private final ITimeSlotRepository timeSlotRepository;
    private final IEmailService emailService;

    public TeacherRequestServiceImpl(TeacherRequestRepository teacherRequestRepository,
                                     ITimeSlotRepository timeSlotRepository,
                                     IEmailService emailService) {
        this.teacherRequestRepository = teacherRequestRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.emailService = emailService;
    }

    // -----------------------------------------------------------------------
    // Helper: find an existing TimeSlot by start+end time or create a new one
    // -----------------------------------------------------------------------
    private Integer resolveTimeSlotId(String startTimeStr, String endTimeStr) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("H:mm");
        LocalTime start = LocalTime.parse(startTimeStr, fmt);
        LocalTime end   = LocalTime.parse(endTimeStr,   fmt);

        Optional<TimeSlot> existing = timeSlotRepository.findByStartTimeAndEndTime(start, end);
        if (existing.isPresent()) {
            return existing.get().getTimeSlotId();
        }

        // No matching slot found – create and persist a new one
        TimeSlot newSlot = new TimeSlot();
        newSlot.setStartTime(start);
        newSlot.setEndTime(end);
        newSlot.setState(true);
        return timeSlotRepository.save(newSlot).getTimeSlotId();
    }

    @Override
    public TeacherRequestsPageDTO getIncomingRequests(Integer userId, Integer statusId, Integer page, Integer size) {
        return teacherRequestRepository.getIncomingRequests(userId, statusId, page, size);
    }

    @Override
    public TeacherActionResponseDTO acceptRequest(Integer userId, Integer requestId, TeacherScheduleRequestDTO dto) {
        Integer timeSlotId = resolveTimeSlotId(dto.getStartTime(), dto.getEndTime());
        TeacherActionResponseDTO response = teacherRequestRepository.acceptRequest(userId, requestId,
                dto.getScheduledDate(), timeSlotId, dto.getModalityId(),
                dto.getEstimatedDuration(), dto.getReason(), dto.getWorkAreaTypeId());

        notifyStatusChange(requestId, response, "Aceptada",
                "Fecha: %s<br/>Hora: %s - %s".formatted(
                        dto.getScheduledDate(), dto.getStartTime(), dto.getEndTime()));
        return response;
    }

    @Override
    public TeacherActionResponseDTO rejectRequest(Integer userId, Integer requestId, String reason) {
        TeacherActionResponseDTO response = teacherRequestRepository.rejectRequest(userId, requestId, reason);
        notifyStatusChange(requestId, response, "Rechazada",
                "Motivo rechazo: %s".formatted(reason != null ? reason : "(no especificado)"));
        return response;
    }

    @Override
    public TeacherActionResponseDTO rescheduleRequest(Integer userId, Integer requestId, TeacherScheduleRequestDTO dto) {
        Integer timeSlotId = resolveTimeSlotId(dto.getStartTime(), dto.getEndTime());
        TeacherActionResponseDTO response = teacherRequestRepository.rescheduleRequest(userId, requestId,
                dto.getScheduledDate(), timeSlotId, dto.getModalityId(),
                dto.getEstimatedDuration(), dto.getReason(), dto.getWorkAreaTypeId());

        notifyStatusChange(requestId, response, "Reprogramada",
                "Nueva fecha: %s<br/>Hora: %s - %s".formatted(
                        dto.getScheduledDate(), dto.getStartTime(), dto.getEndTime()));
        return response;
    }

    @Override
    public TeacherActionResponseDTO cancelSession(Integer userId, Integer requestId, String reason) {
        TeacherActionResponseDTO response = teacherRequestRepository.cancelSession(userId, requestId, reason);
        notifyStatusChange(requestId, response, "Cancelada",
                "Motivo cancelación: %s".formatted(reason != null ? reason : "(no especificado)"));
        return response;
    }

    private void notifyStatusChange(Integer requestId, TeacherActionResponseDTO response, String estadoLegible, String detalleHtml) {
        if (response == null || response.getStatus() == null || response.getStatus().toUpperCase().contains("ERROR")) {
            return;
        }

        StudentRequestSummaryDTO summary = teacherRequestRepository.getRequestSummary(requestId);
        if (summary == null || summary.getStudentEmail() == null || summary.getStudentEmail().isBlank()) {
            return;
        }

        String subject = "SGRA - Solicitud #" + requestId + " " + estadoLegible;
        String body = buildCardEmail(
                "Estado de solicitud",
                "Docente %s".formatted(summary.getTeacherName()),
                """
                        <div style=\"background:#f1f5f9;border:1px solid #e2e8f0;border-radius:10px;padding:14px 16px;margin-bottom:12px;\">
                          <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Solicitud:</strong> #%d</div>
                          <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Asignatura:</strong> %s</div>
                          <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Curso:</strong> %s</div>
                          <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Paralelo:</strong> %s</div>
                          <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Docente:</strong> %s</div>
                          <div style=\"color:#0f172a;font-size:14px;line-height:1.6;\"><strong>Estado:</strong> %s</div>
                        </div>
                        <div style=\"margin-top:6px;color:#374151;font-size:14px;line-height:1.5;\">%s</div>
                        <p style=\"margin:12px 0 0;color:#64748b;font-size:12px;\">Ingresa a SGRA para revisar los detalles de la sesión.</p>
                        """.formatted(requestId, summary.getSubjectName(), summary.getCourseName(), summary.getParallelName(), summary.getTeacherName(), estadoLegible, detalleHtml)
        );

        emailService.sendEmailAsync(summary.getStudentEmail(), subject, body);
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
