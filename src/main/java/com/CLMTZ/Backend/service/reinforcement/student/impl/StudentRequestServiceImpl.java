package com.CLMTZ.Backend.service.reinforcement.student.impl;

import com.CLMTZ.Backend.dto.reinforcement.student.*;
import com.CLMTZ.Backend.repository.reinforcement.student.StudentRequestRepository;
import com.CLMTZ.Backend.service.external.IStorageService;
import com.CLMTZ.Backend.service.reinforcement.student.StudentCatalogService;
import com.CLMTZ.Backend.service.reinforcement.student.StudentRequestService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StudentRequestServiceImpl implements StudentRequestService {

    private final StudentRequestRepository studentRequestRepository;
    private final StudentCatalogService studentCatalogService;
    private final IStorageService storageService;

    public StudentRequestServiceImpl(StudentRequestRepository studentRequestRepository,
                                     StudentCatalogService studentCatalogService,
                                     IStorageService storageService) {
        this.studentRequestRepository = studentRequestRepository;
        this.studentCatalogService = studentCatalogService;
        this.storageService = storageService;
    }

    @Override
    public StudentRequestCreateResponseDTO create(StudentRequestCreateRequestDTO req, Integer userId, MultipartFile[] files) {
        // 1. Resolver el docente automáticamente por el paralelo del estudiante
        StudentSubjectTeacherDTO teacher = studentCatalogService.getTeacherForSubject(req.getSubjectId());
        if (teacher == null) {
            throw new IllegalStateException(
                    "No se encontró un docente asignado para esta asignatura en tu paralelo. " +
                    "Contacta a coordinación."
            );
        }

        // 2. Resolver el periodo activo
        ActivePeriodDTO activePeriod = studentCatalogService.getActivePeriod();
        if (activePeriod == null) {
            throw new IllegalStateException(
                    "No hay un periodo académico activo en este momento. Contacta a coordinación."
            );
        }

        // 3. Crear la solicitud en la base de datos
        Integer requestId = studentRequestRepository.createRequest(
                userId,
                req.getSubjectId(),
                teacher.getTeacherId(),
                req.getSessionTypeId(),
                req.getReason(),
                activePeriod.getPeriodId()
        );

        // 4. Subir archivos a Azure Blob Storage y registrar URLs
        if (files != null) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    String fileUrl = storageService.uploadFiles(file);
                    studentCatalogService.addResourceUrl(requestId, fileUrl);
                }
            }
        }

        // 5. Si es sesión grupal, insertar participantes
        if (req.getParticipantIds() != null && !req.getParticipantIds().isEmpty()) {
            studentRequestRepository.addParticipants(requestId, req.getParticipantIds());
        }

        return new StudentRequestCreateResponseDTO(requestId);
    }
}