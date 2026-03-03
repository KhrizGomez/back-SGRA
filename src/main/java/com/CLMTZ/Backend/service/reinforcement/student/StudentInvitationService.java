package com.CLMTZ.Backend.service.reinforcement.student;

import com.CLMTZ.Backend.dto.reinforcement.student.StudentInvitationHistoryDTO;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentInvitationItemDTO;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentInvitationResponseDTO;

import java.util.List;

/**
 * Servicio para gestionar las invitaciones a tutorías grupales del estudiante.
 */
public interface StudentInvitationService {

    /**
     * Lista las invitaciones a tutorías grupales pendientes de respuesta.
     */
    List<StudentInvitationItemDTO> getPendingInvitations();

    /**
     * Lista el historial de invitaciones grupales ya respondidas.
     */
    List<StudentInvitationHistoryDTO> getInvitationHistory();

    /**
     * Acepta o rechaza una invitación a tutoría grupal.
     */
    StudentInvitationResponseDTO respondInvitation(Integer participantId, Boolean accept);
}

