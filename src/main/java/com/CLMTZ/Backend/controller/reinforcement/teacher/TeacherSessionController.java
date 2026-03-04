package com.CLMTZ.Backend.controller.reinforcement.teacher;

import com.CLMTZ.Backend.config.UserContextHolder;
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
     * Returns sessions with status 'Espera espacio' or 'Reprogramado'.
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
     * RF13: Register virtual meeting link for a scheduled session.
     * Body: { url }
     */
    @PutMapping("/{scheduledId}/virtual-link")
    public ResponseEntity<?> setVirtualLink(
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

            var response = teacherSessionService.setVirtualLink(userId, scheduledId, dto.getUrl());

            if ("ERROR".equals(response.getStatus())) {
                return ResponseEntity.status(409).body(response);
            }
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Error registrando enlace virtual: " + extractBusinessMessage(e.getMessage())));
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
