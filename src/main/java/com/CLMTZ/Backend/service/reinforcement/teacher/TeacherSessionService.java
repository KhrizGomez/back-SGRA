package com.CLMTZ.Backend.service.reinforcement.teacher;

import com.CLMTZ.Backend.dto.reinforcement.teacher.AttendanceItemDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherActionResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TeacherSessionService {
    TeacherActionResponseDTO setVirtualLink(Integer userId, Integer scheduledId, String url);
    TeacherActionResponseDTO markAttendance(Integer userId, Integer scheduledId, Integer performedId,
                                            List<AttendanceItemDTO> attendances);
    TeacherActionResponseDTO registerResult(Integer userId, Integer scheduledId, String observation,
                                            String duration, List<MultipartFile> files);
}
