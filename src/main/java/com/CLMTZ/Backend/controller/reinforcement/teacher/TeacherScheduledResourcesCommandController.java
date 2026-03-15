package com.CLMTZ.Backend.controller.reinforcement.teacher;

import com.CLMTZ.Backend.config.UserContextHolder;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherGeneratedResourceRequestDTO;
import com.CLMTZ.Backend.dto.security.session.UserContext;
import com.CLMTZ.Backend.service.reinforcement.teacher.TeacherSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/teacher/scheduled-resources")
public class TeacherScheduledResourcesCommandController {

    private final TeacherSessionService teacherSessionService;

    public TeacherScheduledResourcesCommandController(TeacherSessionService teacherSessionService) {
        this.teacherSessionService = teacherSessionService;
    }

    @PostMapping("/{scheduledId}")
    public ResponseEntity<?> saveGeneratedResource(
            @PathVariable("scheduledId") Integer scheduledId,
            @RequestBody TeacherGeneratedResourceRequestDTO dto) {
        try {
            UserContext ctx = UserContextHolder.getContext();
            Integer userId = ctx.getUserId();

            if (scheduledId == null || scheduledId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "ID de sesión programada inválido"));
            }
            if (dto == null || dto.getUrl() == null || dto.getUrl().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "La URL del recurso es requerida"));
            }

            var response = teacherSessionService.addGeneratedResource(userId, scheduledId, dto.getUrl().trim());
            if ("ERROR".equals(response.getStatus())) {
                return ResponseEntity.status(409).body(response);
            }
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Error guardando recurso de sesión: " + extractBusinessMessage(e.getMessage())));
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

