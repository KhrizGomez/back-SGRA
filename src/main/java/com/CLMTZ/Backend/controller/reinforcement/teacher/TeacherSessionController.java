package com.CLMTZ.Backend.controller.reinforcement.teacher;

import com.CLMTZ.Backend.config.UserContextHolder;
import com.CLMTZ.Backend.dto.reinforcement.teacher.AttendanceItemDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherAttendanceMarkDTO;
// import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherRegisterResultDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherVirtualLinkDTO;
import com.CLMTZ.Backend.dto.security.session.UserContext;
import com.CLMTZ.Backend.service.reinforcement.teacher.TeacherSessionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Teacher session management controller.
 * RF13: Register virtual meeting link
 * RF16: Mark attendance per participant
 * RF17: Register session results and upload resources
 */
@RestController
@RequestMapping("/api/teacher/sessions")
public class TeacherSessionController {

    private final TeacherSessionService teacherSessionService;

    public TeacherSessionController(TeacherSessionService teacherSessionService) {
        this.teacherSessionService = teacherSessionService;
    }

    /**
     * GET /api/teacher/sessions/active
     * Returns sessions with status 'Espera espacio', 'Reprogramado' or 'Programado'.
     */
    @GetMapping("/active")
    public ResponseEntity<?> getActiveSessions() {
        try {
            UserContext ctx = UserContextHolder.getContext();
            Integer userId = ctx.getUserId();
            return ResponseEntity.ok(teacherSessionService.getActiveSessions(userId));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Error obteniendo sesiones activas: " + e.getMessage()));
        }
    }

    /**
     * GET /api/teacher/sessions/{scheduledId}/participants
     * Lista los participantes de la sesión con su estado de asistencia actual.
     */
    @GetMapping("/{scheduledId}/participants")
    public ResponseEntity<?> getSessionAttendance(
            @PathVariable("scheduledId") Integer scheduledId) {
        try {
            UserContext ctx = UserContextHolder.getContext();
            Integer userId = ctx.getUserId();
            if (scheduledId == null || scheduledId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid scheduledId"));
            }
            return ResponseEntity.ok(teacherSessionService.getSessionAttendance(userId, scheduledId));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Error obteniendo participantes: " + e.getMessage()));
        }
    }

    /**
     * PUT /api/teacher/sessions/{scheduledId}/participants
     * Registra/actualiza la asistencia de todos los participantes en tbasistenciasrefuerzos.
     * Body: [ { "participantId": 1, "attended": true }, ... ]
     */
    @PutMapping("/{scheduledId}/participants")
    public ResponseEntity<?> updateSessionAttendance(
            @PathVariable("scheduledId") Integer scheduledId,
            @RequestBody List<AttendanceItemDTO> attendances) {
        try {
            UserContext ctx = UserContextHolder.getContext();
            Integer userId = ctx.getUserId();
            if (scheduledId == null || scheduledId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid scheduledId"));
            }
            if (attendances == null || attendances.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "La lista de participantes no puede estar vacía"));
            }
            var response = teacherSessionService.updateSessionAttendance(userId, scheduledId, attendances);
            if ("ERROR".equals(response.getStatus())) {
                return ResponseEntity.status(409).body(response);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Error registrando asistencia: " + e.getMessage()));
        }
    }

    /**
     * RF13: Manage links for a scheduled session (CRUD).
     * Body: { url }
     */
    @GetMapping("/{scheduledId}/links")
    public ResponseEntity<?> getSessionLinks(@PathVariable("scheduledId") Integer scheduledId) {
        try {
            UserContext ctx = UserContextHolder.getContext();
            Integer userId = ctx.getUserId();

            if (scheduledId == null || scheduledId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid scheduledId"));
            }
            return ResponseEntity.ok(teacherSessionService.getSessionLinks(userId, scheduledId));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Error obteniendo enlaces: " + extractBusinessMessage(e.getMessage())));
        }
    }

    @PostMapping("/{scheduledId}/links")
    public ResponseEntity<?> addLink(
            @PathVariable("scheduledId") Integer scheduledId,
            @RequestBody TeacherVirtualLinkDTO dto) {
        try {
            UserContext ctx = UserContextHolder.getContext();
            Integer userId = ctx.getUserId();

            if (scheduledId == null || scheduledId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid scheduledId"));
            }
            if (dto.getUrl() == null || dto.getUrl().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "La URL del enlace es requerida"));
            }

            var response = teacherSessionService.addLink(userId, scheduledId, dto.getUrl());

            if ("ERROR".equals(response.getStatus())) {
                return ResponseEntity.status(409).body(response);
            }
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Error agregando enlace: " + extractBusinessMessage(e.getMessage())));
        }
    }

    @DeleteMapping("/{scheduledId}/links")
    public ResponseEntity<?> deleteLink(
            @PathVariable("scheduledId") Integer scheduledId,
            @RequestParam("url") String url) {
        try {
            UserContext ctx = UserContextHolder.getContext();
            Integer userId = ctx.getUserId();

            if (scheduledId == null || scheduledId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid scheduledId"));
            }
            if (url == null || url.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "url es requerida"));
            }

            teacherSessionService.deleteLink(userId, scheduledId, url);
            return ResponseEntity.ok(Map.of("message", "Enlace eliminado correctamente"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Error eliminando enlace: " + extractBusinessMessage(e.getMessage())));
        }
    }

    /**
     * RF16: Mark attendance for each participant of a performed session.
     * Body: { performedId, attendances: [{ participantId, attended }] }
     */
    @PostMapping("/{scheduledId}/attendance")
    public ResponseEntity<?> markAttendance(
            @PathVariable("scheduledId") Integer scheduledId,
            @RequestBody TeacherAttendanceMarkDTO dto) {
        try {
            UserContext ctx = UserContextHolder.getContext();
            Integer userId = ctx.getUserId();

            if (scheduledId == null || scheduledId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid scheduledId"));
            }
            if (dto.getPerformedId() == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "performedId es requerido"));
            }
            if (dto.getAttendances() == null || dto.getAttendances().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "La lista de asistentes no puede estar vacía"));
            }

            var response = teacherSessionService.markAttendance(userId, scheduledId,
                    dto.getPerformedId(), dto.getAttendances());

            if ("ERROR".equals(response.getStatus())) {
                return ResponseEntity.status(409).body(response);
            }
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Error registrando asistencia: " + extractBusinessMessage(e.getMessage())));
        }
    }

    /**
     * RF17: Register session results and optionally attach resource files.
     * Multipart form: observation (text), duration (text "HH:mm"), files[] (optional)
     */
    @PostMapping(value = "/{scheduledId}/performed", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registerResult(
            @PathVariable("scheduledId") Integer scheduledId,
            @RequestParam("observation") String observation,
            @RequestParam("duration") String duration,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {
        try {
            UserContext ctx = UserContextHolder.getContext();
            Integer userId = ctx.getUserId();

            if (scheduledId == null || scheduledId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid scheduledId"));
            }
            if (observation == null || observation.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "La observación es requerida"));
            }
            if (duration == null || duration.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "La duración es requerida"));
            }

            var response = teacherSessionService.registerResult(userId, scheduledId,
                    observation, duration, files);

            if ("ERROR".equals(response.getStatus())) {
                return ResponseEntity.status(409).body(response);
            }
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Error registrando resultado: " + extractBusinessMessage(e.getMessage())));
        }
    }

    /**
     * GET /api/teacher/sessions/{scheduledId}/request-resources
     * Obtiene los recursos (archivos) de la solicitud original asociada a la sesión programada.
     */
    @GetMapping("/{scheduledId}/request-resources")
    public ResponseEntity<?> getSessionRequestResources(@PathVariable("scheduledId") Integer scheduledId) {
        try {
            UserContext ctx = UserContextHolder.getContext();
            Integer userId = ctx.getUserId();

            if (scheduledId == null || scheduledId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid scheduledId"));
            }

            List<String> resources = teacherSessionService.getSessionRequestResources(userId, scheduledId);
            return ResponseEntity.ok(resources);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Error obteniendo recursos de la solicitud: " + extractBusinessMessage(e.getMessage())));
        }
    }

    /**
     * GET /api/teacher/sessions/{scheduledId}/resources
     * Obtiene los recursos generados/subidos por el docente para esta sesión programada.
     */
    @GetMapping("/{scheduledId}/resources")
    public ResponseEntity<?> getSessionResources(@PathVariable("scheduledId") Integer scheduledId) {
        try {
            UserContext ctx = UserContextHolder.getContext();
            Integer userId = ctx.getUserId();

            if (scheduledId == null || scheduledId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid scheduledId"));
            }

            List<String> resources = teacherSessionService.getSessionResources(userId, scheduledId);
            return ResponseEntity.ok(resources);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Error obteniendo recursos de la sesión: " + extractBusinessMessage(e.getMessage())));
        }
    }

    /**
     * POST /api/teacher/sessions/{scheduledId}/resources
     * Sube un nuevo recurso para la sesión programada.
     * Consumes: multipart/form-data ("file")
     */
    @PostMapping(value = "/{scheduledId}/resources", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadSessionResource(
            @PathVariable("scheduledId") Integer scheduledId,
            @RequestParam("file") MultipartFile file) {
        try {
            UserContext ctx = UserContextHolder.getContext();
            Integer userId = ctx.getUserId();

            if (scheduledId == null || scheduledId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid scheduledId"));
            }

            var response = teacherSessionService.uploadSessionResource(userId, scheduledId, file);

            if ("ERROR".equals(response.getStatus())) {
                return ResponseEntity.status(409).body(response);
            }
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Error subiendo recurso: " + extractBusinessMessage(e.getMessage())));
        }
    }

    /**
     * DELETE /api/teacher/sessions/{scheduledId}/resources
     * Elimina un recurso de la sesión programada.
     */
    @DeleteMapping("/{scheduledId}/resources")
    public ResponseEntity<?> deleteSessionResource(
            @PathVariable("scheduledId") Integer scheduledId,
            @RequestParam("fileUrl") String fileUrl) {
        try {
            UserContext ctx = UserContextHolder.getContext();
            Integer userId = ctx.getUserId();

            if (scheduledId == null || scheduledId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid scheduledId"));
            }
            if (fileUrl == null || fileUrl.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "fileUrl es requerido"));
            }

            teacherSessionService.deleteSessionResource(userId, scheduledId, fileUrl);
            return ResponseEntity.ok(Map.of("message", "Recurso eliminado correctamente"));
        } catch (IllegalArgumentException e) {
             // Si el recurso no existe o no pertenece, puede ser 404 o 400. 
             // Dado que es delete, 404 seria mas correcto si no existe, pero aqui usamos 400 por simplicidad
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Error eliminando recurso: " + extractBusinessMessage(e.getMessage())));
        }
    }

    private String extractBusinessMessage(String fullMessage) {
        if (fullMessage == null) return "Operation failed";
        if (fullMessage.contains("ERROR:")) {
            int errorIndex = fullMessage.indexOf("ERROR:");
            String afterError = fullMessage.substring(errorIndex + 6).trim();
            int newlineIndex = afterError.indexOf("\n");
            if (newlineIndex > 0) return afterError.substring(0, newlineIndex).trim();
            return afterError;
        }
        return fullMessage;
    }
}
