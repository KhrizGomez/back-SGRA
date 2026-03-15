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
    public TeacherActionResponseDTO addLink(Integer userId, Integer scheduledId, String url) {
        return teacherSessionRepository.addLink(userId, scheduledId, url);
    }

    @Override
    public void deleteLink(Integer userId, Integer scheduledId, String url) {
        teacherSessionRepository.deleteLink(userId, scheduledId, url);
    }

    @Override
    public List<String> getSessionLinks(Integer userId, Integer scheduledId) {
        return teacherSessionRepository.getSessionLinks(userId, scheduledId);
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
                    teacherSessionRepository.addResource(userId, scheduledId, fileUrl);
                }
            }
        }

        return new TeacherActionResponseDTO(performedId, "RESULT_REGISTERED",
                "Resultado de sesión registrado correctamente");
    }

    @Override
    public List<String> getSessionRequestResources(Integer userId, Integer scheduledId) {
        return teacherSessionRepository.getSessionRequestResources(userId, scheduledId);
    }

    @Override
    public List<String> getSessionResources(Integer userId, Integer scheduledId) {
        return teacherSessionRepository.getSessionResources(userId, scheduledId);
    }

    @Override
    public TeacherActionResponseDTO uploadSessionResource(Integer userId, Integer scheduledId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo no puede estar vacío");
        }

        String fileUrl = storageService.uploadFiles(file);
        return teacherSessionRepository.addResource(userId, scheduledId, fileUrl);
    }

    @Override
    public void deleteSessionResource(Integer userId, Integer scheduledId, String fileUrl) {
        teacherSessionRepository.deleteResource(userId, scheduledId, fileUrl);
        
        try {
            // Intentar eliminar del Storage si es posible extraer el nombre
            java.net.URI uri = new java.net.URI(fileUrl);
            String path = uri.getPath();
            String fileName = path.substring(path.lastIndexOf('/') + 1);
            if (fileName != null && !fileName.isEmpty()) {
                // Decodificar por si tiene espacios u otros caracteres
                fileName = java.net.URLDecoder.decode(fileName, java.nio.charset.StandardCharsets.UTF_8);
                storageService.deleteFile(fileName);
            }
        } catch (Exception e) {
            // Solo loguear, lo importante es que se borró el registro de BD
            System.err.println("Advertencia: No se pudo eliminar el archivo físico del storage: " + e.getMessage());
        }
    }
}
