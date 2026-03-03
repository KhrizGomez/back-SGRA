package com.CLMTZ.Backend.repository.reinforcement.student;

import com.CLMTZ.Backend.dto.reinforcement.student.StudentInvitationHistoryDTO;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentInvitationItemDTO;

import java.util.List;

/**
 * Repositorio para las invitaciones a tutorías grupales del estudiante.
 */
public interface StudentInvitationRepository {

    /**
     * Lista las invitaciones a tutorías grupales pendientes de respuesta.
     */
    List<StudentInvitationItemDTO> listPendingInvitations(Integer userId);

    /**
     * Lista el historial de invitaciones grupales ya respondidas (aceptadas).
     */
    List<StudentInvitationHistoryDTO> listInvitationHistory(Integer userId);

    /**
     * Acepta o rechaza una invitación a tutoría grupal.
     */
    Boolean respondInvitation(Integer userId, Integer participantId, Boolean accept);
}

