package com.CLMTZ.Backend.repository.reinforcement.student;

import com.CLMTZ.Backend.dto.reinforcement.student.StudentInvitationItemDTO;

import java.util.List;

/**
 * Repositorio para las invitaciones a tutorías grupales del estudiante.
 */
public interface StudentInvitationRepository {

    /**
     * Lista las invitaciones a tutorías grupales pendientes de respuesta.
     *
     * @param userId ID del usuario autenticado
     * @return Lista de invitaciones pendientes
     */
    List<StudentInvitationItemDTO> listPendingInvitations(Integer userId);

    /**
     * Acepta o rechaza una invitación a tutoría grupal.
     *
     * @param userId        ID del usuario autenticado
     * @param participantId Id del registro en tbparticipantes
     * @param accept        true = acepta, false = rechaza
     * @return true si la operación fue exitosa
     */
    Boolean respondInvitation(Integer userId, Integer participantId, Boolean accept);
}

