package com.CLMTZ.Backend.dto.reinforcement.student;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * DTO para el historial de invitaciones grupales respondidas.
 * Mapeado desde fn_sl_historial_invitaciones_estudiante.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentInvitationHistoryDTO {
    private Integer participantId;
    private Integer requestId;
    private String subjectName;
    private Short semester;
    private String requesterName;
    private String requesterEmail;
    private String teacherName;
    private String sessionType;
    private String reason;
    private Timestamp requestDate;
    private String invitationStatus;   // "Aceptada" o "Rechazada"
    private String requestStatus;      // Estado de la solicitud (Pendiente, Aceptada, etc.)
    private Long totalInvited;
    private Long totalAccepted;
}

