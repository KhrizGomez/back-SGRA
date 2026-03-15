package com.CLMTZ.Backend.repository.reinforcement.teacher;

import com.CLMTZ.Backend.dto.reinforcement.teacher.AttendanceItemDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.ParticipantAttendanceDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherActionResponseDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherActiveSessionItemDTO;

import java.util.List;

public interface TeacherSessionRepository {
    List<TeacherActiveSessionItemDTO> getActiveSessions(Integer userId);
    List<ParticipantAttendanceDTO> getSessionAttendance(Integer userId, Integer scheduledId);
    List<String> getSessionResources(Integer userId, Integer scheduledId);
    List<String> getSessionRequestResources(Integer userId, Integer scheduledId);
    TeacherActionResponseDTO updateSessionAttendance(Integer userId, Integer scheduledId, List<AttendanceItemDTO> attendances);
    TeacherActionResponseDTO setVirtualLink(Integer userId, Integer scheduledId, String url);
    TeacherActionResponseDTO markAttendance(Integer userId, Integer scheduledId, Integer performedId,
                                            List<AttendanceItemDTO> attendances);
    Integer registerResult(Integer userId, Integer scheduledId, String observation, String duration);
    TeacherActionResponseDTO addResource(Integer userId, Integer scheduledId, String fileUrl);
}
