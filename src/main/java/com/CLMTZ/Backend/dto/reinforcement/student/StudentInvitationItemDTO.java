package com.CLMTZ.Backend.dto.reinforcement.student;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * DTO para una invitación a tutoría grupal pendiente de respuesta.
 * Mapeado desde fn_sl_invitaciones_grupales_estudiante.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentInvitationItemDTO {
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
    private Long totalInvited;
    private Long totalAccepted;
}

