package com.CLMTZ.Backend.controller.reinforcement.student;

import com.CLMTZ.Backend.dto.reinforcement.student.*;
import com.CLMTZ.Backend.service.reinforcement.student.StudentCatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student/catalogs")
public class StudentCatalogController {

    private final StudentCatalogService studentCatalogService;

    public StudentCatalogController(StudentCatalogService studentCatalogService) {
        this.studentCatalogService = studentCatalogService;
    }

    /**
     * Obtiene las asignaturas en las que el estudiante autenticado está matriculado
     * en el periodo activo.
     */
    @GetMapping("/subjects")
    public ResponseEntity<?> getSubjects() {
        try {
            List<SubjectItemDTO> subjects = studentCatalogService.getEnrolledSubjects();
            return ResponseEntity.ok(subjects);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error al obtener asignaturas: " + e.getMessage()));
        }
    }

    /**
     * Obtiene el docente asignado al paralelo del estudiante para una asignatura.
     * Retorna el docente o un mensaje indicando que no hay docente asignado.
     */
    @GetMapping("/subjects/{subjectId}/teacher")
    public ResponseEntity<?> getTeacherBySubject(@PathVariable("subjectId") Integer subjectId) {
        try {
            if (subjectId == null || subjectId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "ID de asignatura inválido"));
            }
            StudentSubjectTeacherDTO teacher = studentCatalogService.getTeacherForSubject(subjectId);
            if (teacher == null) {
                return ResponseEntity.ok(Map.of(
                        "found", false,
                        "message", "No se encontró un docente asignado para esta asignatura en tu paralelo"
                ));
            }
            return ResponseEntity.ok(teacher);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error al obtener docente: " + e.getMessage()));
        }
    }

    /**
     * Lista los tipos de sesión (Individual, Grupal).
     */
    @GetMapping("/sessionTypes")
    public ResponseEntity<?> getSessionTypes() {
        try {
            List<SessionTypeItemDTO> sessionTypes = studentCatalogService.getSessionTypes();
            return ResponseEntity.ok(sessionTypes);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Error al obtener tipos de sesión: " + e.getMessage()));
        }
    }

    /**
     * Obtiene el periodo académico activo.
     */
    @GetMapping("/active-period")
    public ResponseEntity<?> getActivePeriod() {
        try {
            ActivePeriodDTO period = studentCatalogService.getActivePeriod();
            if (period == null) {
                return ResponseEntity.ok(Map.of(
                        "found", false,
                        "message", "No hay un periodo académico activo actualmente"
                ));
            }
            return ResponseEntity.ok(period);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error al obtener periodo activo: " + e.getMessage()));
        }
    }

    /**
     * Obtiene compañeros matriculados en la misma asignatura.
     * Excluye al estudiante autenticado.
     */
    @GetMapping("/subjects/{subjectId}/classmates")
    public ResponseEntity<?> getClassmatesBySubject(@PathVariable("subjectId") Integer subjectId) {
        try {
            if (subjectId == null || subjectId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "ID de asignatura inválido"));
            }
            List<ClassmateItemDTO> classmates = studentCatalogService.getClassmatesBySubject(subjectId);
            return ResponseEntity.ok(classmates);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Error al obtener compañeros: " + e.getMessage()));
        }
    }
}