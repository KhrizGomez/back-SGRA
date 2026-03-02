package com.CLMTZ.Backend.service.reinforcement.student;

import com.CLMTZ.Backend.dto.reinforcement.student.*;

import java.util.List;

public interface StudentCatalogService {

    /**
     * Obtiene las asignaturas en las que el estudiante está matriculado (periodo activo).
     */
    List<SubjectItemDTO> getEnrolledSubjects();

    /**
     * Obtiene el docente asignado al paralelo del estudiante para una asignatura.
     * Retorna null si no hay docente asignado.
     */
    StudentSubjectTeacherDTO getTeacherForSubject(Integer subjectId);

    /**
     * Lista tipos de sesión (Individual, Grupal).
     */
    List<SessionTypeItemDTO> getSessionTypes();

    /**
     * Obtiene el periodo académico activo.
     */
    ActivePeriodDTO getActivePeriod();

    /**
     * Obtiene los compañeros matriculados en la misma asignatura.
     * Excluye al usuario autenticado.
     */
    List<ClassmateItemDTO> getClassmatesBySubject(Integer subjectId);

    /**
     * Inserta una URL de recurso/archivo para una solicitud.
     */
    void addResourceUrl(Integer requestId, String fileUrl);
}