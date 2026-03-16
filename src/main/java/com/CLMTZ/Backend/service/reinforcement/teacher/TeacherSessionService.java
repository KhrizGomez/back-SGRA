package com.CLMTZ.Backend.service.reinforcement.teacher;

import com.CLMTZ.Backend.dto.reinforcement.teacher.AttendanceItemDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.ParticipantAttendanceDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherActionResponseDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherActiveSessionItemDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TeacherSessionService {
    List<TeacherActiveSessionItemDTO> getActiveSessions(Integer userId);
    List<ParticipantAttendanceDTO> getSessionAttendance(Integer userId, Integer scheduledId);
    List<String> getSessionResources(Integer userId, Integer scheduledId);
    List<String> getSessionLinks(Integer userId, Integer scheduledId); // Newly added
    List<String> getSessionRequestResources(Integer userId, Integer scheduledId);
    TeacherActionResponseDTO updateSessionAttendance(Integer userId, Integer scheduledId, List<AttendanceItemDTO> attendances);
    TeacherActionResponseDTO addLink(Integer userId, Integer scheduledId, String url); // Replaces setVirtualLink
    void deleteLink(Integer userId, Integer scheduledId, String url); // Newly added
    TeacherActionResponseDTO markAttendance(Integer userId, Integer scheduledId, Integer performedId,
                                            List<AttendanceItemDTO> attendances);
    TeacherActionResponseDTO registerResult(Integer userId, Integer scheduledId, String observation,
                                            String duration, List<MultipartFile> files);
    TeacherActionResponseDTO uploadSessionResource(Integer userId, Integer scheduledId, MultipartFile file);
    void deleteSessionResource(Integer userId, Integer scheduledId, String fileUrl);
}
