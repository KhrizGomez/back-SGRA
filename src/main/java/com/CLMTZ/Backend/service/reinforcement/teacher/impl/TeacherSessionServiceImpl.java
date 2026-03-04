package com.CLMTZ.Backend.service.reinforcement.teacher.impl;

import com.CLMTZ.Backend.dto.reinforcement.teacher.AttendanceItemDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.ParticipantAttendanceDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherActionResponseDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherActiveSessionItemDTO;
import com.CLMTZ.Backend.repository.reinforcement.teacher.TeacherSessionRepository;
import com.CLMTZ.Backend.service.external.IStorageService;
import com.CLMTZ.Backend.service.reinforcement.teacher.TeacherSessionService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class TeacherSessionServiceImpl implements TeacherSessionService {

    private final TeacherSessionRepository teacherSessionRepository;
    private final IStorageService storageService;

    public TeacherSessionServiceImpl(TeacherSessionRepository teacherSessionRepository,
                                     IStorageService storageService) {
        this.teacherSessionRepository = teacherSessionRepository;
        this.storageService = storageService;
    }

    @Override
    public List<TeacherActiveSessionItemDTO> getActiveSessions(Integer userId) {
        return teacherSessionRepository.getActiveSessions(userId);
    }

    @Override
    public List<ParticipantAttendanceDTO> getSessionAttendance(Integer userId, Integer scheduledId) {
        return teacherSessionRepository.getSessionAttendance(userId, scheduledId);
    }

    @Override
    public TeacherActionResponseDTO updateSessionAttendance(Integer userId, Integer scheduledId,
                                                            List<AttendanceItemDTO> attendances) {
        return teacherSessionRepository.updateSessionAttendance(userId, scheduledId, attendances);
    }

    @Override
    public TeacherActionResponseDTO setVirtualLink(Integer userId, Integer scheduledId, String url) {
        return teacherSessionRepository.setVirtualLink(userId, scheduledId, url);
    }

    @Override
    public TeacherActionResponseDTO markAttendance(Integer userId, Integer scheduledId, Integer performedId,
                                                   List<AttendanceItemDTO> attendances) {
        return teacherSessionRepository.markAttendance(userId, scheduledId, performedId, attendances);
    }

    @Override
    public TeacherActionResponseDTO registerResult(Integer userId, Integer scheduledId, String observation,
                                                   String duration, List<MultipartFile> files) {
        Integer performedId = teacherSessionRepository.registerResult(userId, scheduledId, observation, duration);

        if (files != null) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    String fileUrl = storageService.uploadFiles(file);
                    teacherSessionRepository.addResource(scheduledId, fileUrl);
                }
            }
        }

        return new TeacherActionResponseDTO(performedId, "RESULT_REGISTERED",
                "Resultado de sesión registrado correctamente");
    }
}
