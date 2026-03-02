package com.CLMTZ.Backend.controller.reinforcement.teacher;

import com.CLMTZ.Backend.config.UserContextHolder;
import com.CLMTZ.Backend.dto.security.session.UserContext;
import com.CLMTZ.Backend.service.reinforcement.teacher.TeacherHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Teacher history controller.
 * RF18: View session history (subject, date, modality, duration, final state)
 */
@RestController
@RequestMapping("/api/teacher/history")
public class TeacherHistoryController {

    private final TeacherHistoryService teacherHistoryService;

    public TeacherHistoryController(TeacherHistoryService teacherHistoryService) {
        this.teacherHistoryService = teacherHistoryService;
    }

    /**
     * RF18: Get paginated session history for the authenticated teacher.
     */
    @GetMapping("/sessions")
    public ResponseEntity<?> getSessionHistory(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        try {
            UserContext ctx = UserContextHolder.getContext();
            Integer userId = ctx.getUserId();

            if (page < 1) page = 1;
            if (size < 1 || size > 100) size = 10;

            return ResponseEntity.ok(
                    teacherHistoryService.getSessionHistory(userId, page, size));

        } catch (Exception e) {
            String message = e.getMessage();
            if (message != null) {
                String lowerMessage = message.toLowerCase();
                if (lowerMessage.contains("no pertenece") || lowerMessage.contains("unauthorized")) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("message", extractBusinessMessage(message)));
                }
            }
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Error retrieving session history: " + extractBusinessMessage(message)));
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
