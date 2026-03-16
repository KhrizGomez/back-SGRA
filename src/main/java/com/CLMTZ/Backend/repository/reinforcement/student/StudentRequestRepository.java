package com.CLMTZ.Backend.repository.reinforcement.student;

import java.util.List;

public interface StudentRequestRepository {

    /**
     * Crea una nueva solicitud de refuerzo simplificada.
     * Solo inserta: estudiante, asignatura, docente, tipo sesión, motivo, periodo, estado inicial.
     *
     * @param userId        ID del usuario autenticado
     * @param subjectId     ID de la asignatura
     * @param teacherId     ID del docente (resuelto por paralelo)
     * @param sessionTypeId ID del tipo de sesión
     * @param reason        Motivo de la solicitud
     * @param periodId      ID del periodo activo
     * @return ID de la solicitud creada
     */
    Integer createRequest(Integer userId, Integer subjectId, Integer teacherId,
                          Integer sessionTypeId, String reason, Integer periodId);

    /**
     * Inserta participantes para una solicitud de sesión grupal.
     *
     * @param requestId  ID de la solicitud creada
     * @param studentIds Lista de IDs de estudiantes participantes
     */
    void addParticipants(Integer requestId, List<Integer> studentIds);

    /**
     * Obtiene datos básicos de la solicitud para notificaciones.
     */
    com.CLMTZ.Backend.dto.reinforcement.student.StudentRequestSummaryDTO getRequestSummary(Integer requestId);
}