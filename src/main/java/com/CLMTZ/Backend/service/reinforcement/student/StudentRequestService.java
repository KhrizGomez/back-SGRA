package com.CLMTZ.Backend.service.reinforcement.student;

import com.CLMTZ.Backend.dto.reinforcement.student.StudentRequestCreateRequestDTO;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentRequestCreateResponseDTO;
import org.springframework.web.multipart.MultipartFile;

public interface StudentRequestService {
    /**
     * Crea una nueva solicitud de refuerzo simplificada.
     * Resuelve automáticamente el docente (por paralelo) y el periodo activo.
     * Sube los archivos a Azure Blob Storage y los asocia a la solicitud.
     *
     * @param req    Datos de la solicitud (subjectId, sessionTypeId, reason, participantIds)
     * @param userId ID del usuario autenticado
     * @param files  Archivos opcionales a adjuntar
     * @return DTO con el ID de la solicitud creada
     */
    StudentRequestCreateResponseDTO create(StudentRequestCreateRequestDTO req, Integer userId, MultipartFile[] files);
}