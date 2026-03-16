package com.CLMTZ.Backend.repository.reinforcement.teacher;

import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherActionResponseDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherRequestsPageDTO;

public interface TeacherRequestRepository {
    TeacherRequestsPageDTO getIncomingRequests(Integer userId, Integer statusId, Integer page, Integer size);
    TeacherActionResponseDTO acceptRequest(Integer userId, Integer requestId, String scheduledDate,
                                           Integer timeSlotId, Integer modalityId, String estimatedDuration,
                                           String reason, Integer workAreaTypeId);
    TeacherActionResponseDTO rejectRequest(Integer userId, Integer requestId, String reason);
    TeacherActionResponseDTO rescheduleRequest(Integer userId, Integer requestId, String scheduledDate,
                                               Integer timeSlotId, Integer modalityId, String estimatedDuration,
                                               String reason, Integer workAreaTypeId);
    TeacherActionResponseDTO cancelSession(Integer userId, Integer scheduledId, String reason);

    /**
     * Datos de contacto del estudiante para notificaciones.
     */
    com.CLMTZ.Backend.dto.reinforcement.student.StudentRequestSummaryDTO getRequestSummary(Integer requestId);
}
