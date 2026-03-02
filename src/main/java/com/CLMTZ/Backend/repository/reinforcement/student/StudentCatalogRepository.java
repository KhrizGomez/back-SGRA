package com.CLMTZ.Backend.repository.reinforcement.student;

import com.CLMTZ.Backend.dto.reinforcement.student.*;

import java.util.List;

public interface StudentCatalogRepository {

    /**
     * Lista las asignaturas en las que el estudiante está matriculado en el periodo activo.
     *
     * @param userId ID del usuario autenticado
     * @return Lista de asignaturas matriculadas
     */
    List<SubjectItemDTO> listEnrolledSubjects(Integer userId);

    /**
     * Obtiene el docente asignado al paralelo del estudiante para una asignatura.
     * Filtra por el paralelo que tiene el estudiante en tbdetallematricula y busca en tbclases
     * el docente correspondiente a ese paralelo + asignatura + periodo activo.
     *
     * @param userId    ID del usuario autenticado
     * @param subjectId ID de la asignatura
     * @return DTO con datos del docente, o null si no hay docente asignado
     */
    StudentSubjectTeacherDTO getTeacherForStudentSubject(Integer userId, Integer subjectId);

    /**
     * Lista los tipos de sesión (Individual, Grupal).
     */
    List<SessionTypeItemDTO> listSessionTypes();

    /**
     * Obtiene el periodo académico activo (estado=true y fecha actual entre inicio y fin).
     *
     * @return DTO con el periodo activo, o null si no hay periodo activo
     */
    ActivePeriodDTO getActivePeriod();

    /**
     * Lista compañeros matriculados en la misma asignatura, excluyendo al usuario actual.
     *
     * @param subjectId     ID de la asignatura
     * @param currentUserId ID del usuario actual (para excluirlo de la lista)
     * @return Lista de compañeros con studentId, nombre completo y email
     */
    List<ClassmateItemDTO> listClassmatesBySubject(Integer subjectId, Integer currentUserId);

    /**
     * Inserta una URL de recurso/archivo para una solicitud de refuerzo.
     *
     * @param requestId ID de la solicitud
     * @param fileUrl   URL del archivo subido a Azure Blob Storage
     */
    void addResourceUrl(Integer requestId, String fileUrl);
}