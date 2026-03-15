package com.CLMTZ.Backend.controller.reinforcement.teacher;

import com.CLMTZ.Backend.config.UserContextHolder;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherScheduledResourcesDTO;
import com.CLMTZ.Backend.dto.security.session.UserContext;
import com.CLMTZ.Backend.service.reinforcement.teacher.TeacherSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/teacher/scheduled-resources")
public class TeacherScheduledResourcesQueryController {

    private final TeacherSessionService teacherSessionService;

    public TeacherScheduledResourcesQueryController(TeacherSessionService teacherSessionService) {
        this.teacherSessionService = teacherSessionService;
    }

    @GetMapping("/{scheduledId}")
    public ResponseEntity<?> getScheduledResources(@PathVariable("scheduledId") Integer scheduledId) {
        try {
            UserContext ctx = UserContextHolder.getContext();
            Integer userId = ctx.getUserId();

            if (scheduledId == null || scheduledId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "ID de sesión programada inválido"));
            }

            TeacherScheduledResourcesDTO response = new TeacherScheduledResourcesDTO(
                    scheduledId,
                    teacherSessionService.getSessionResources(userId, scheduledId)
            );
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Error obteniendo recursos de la sesión: " + extractBusinessMessage(e.getMessage())));
        }
    }

    @GetMapping("/{scheduledId}/request-resources")
    public ResponseEntity<?> getRelatedRequestResources(@PathVariable("scheduledId") Integer scheduledId) {
        try {
            UserContext ctx = UserContextHolder.getContext();
            Integer userId = ctx.getUserId();

            if (scheduledId == null || scheduledId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "ID de sesión programada inválido"));
            }

            TeacherScheduledResourcesDTO response = new TeacherScheduledResourcesDTO(
                    scheduledId,
                    teacherSessionService.getSessionRequestResources(userId, scheduledId)
            );
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Error obteniendo recursos de solicitudes relacionadas: " + extractBusinessMessage(e.getMessage())));
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

