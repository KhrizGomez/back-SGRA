package com.CLMTZ.Backend.service.reinforcement.student;

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
     * Acepta o rechaza una invitación a tutoría grupal.
     *
     * @param participantId ID del registro en tbparticipantes
     * @param accept        true = acepta, false = rechaza
     * @return DTO con resultado de la operación
     */
    StudentInvitationResponseDTO respondInvitation(Integer participantId, Boolean accept);
}

