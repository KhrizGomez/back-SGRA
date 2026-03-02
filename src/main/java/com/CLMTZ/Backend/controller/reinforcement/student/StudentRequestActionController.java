package com.CLMTZ.Backend.controller.reinforcement.student;

import com.CLMTZ.Backend.config.UserContextHolder;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentCancelRequestResponseDTO;
import com.CLMTZ.Backend.dto.security.session.UserContext;
import com.CLMTZ.Backend.service.reinforcement.student.StudentRequestActionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/student/requests")
public class StudentRequestActionController {

    private final StudentRequestActionService studentRequestActionService;

    public StudentRequestActionController(StudentRequestActionService studentRequestActionService) {
        this.studentRequestActionService = studentRequestActionService;
    }

    @PutMapping("/{requestId}/cancel")
    public ResponseEntity<?> cancelRequest(@PathVariable("requestId") Integer requestId) {
        try {
            UserContext ctx = UserContextHolder.getContext();
            Integer userId = ctx.getUserId();


            if (requestId == null || requestId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid requestId parameter"));
            }

            StudentCancelRequestResponseDTO response = studentRequestActionService.cancelRequest(userId, requestId);

            if ("NOT_MODIFIED".equals(response.getStatus())) {
                return ResponseEntity.status(409).body(response);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            String message = e.getMessage();
            if (message != null) {
                String lowerMessage = message.toLowerCase();
                if (lowerMessage.contains("no pertenece") || lowerMessage.contains("does not belong") ||
                    lowerMessage.contains("unauthorized") || lowerMessage.contains("permission")) {
                    return ResponseEntity.badRequest().body(Map.of("message", extractBusinessMessage(message)));
                }if (lowerMessage.contains("ya cancelada") || lowerMessage.contains("already cancelled") ||
                    lowerMessage.contains("cannot cancel") || lowerMessage.contains("no se puede cancelar") ||
                    lowerMessage.contains("estado")) {
                    return ResponseEntity.status(409).body(Map.of("message", extractBusinessMessage(message)));
                }
            }
            return ResponseEntity.status(500).body(Map.of("message", "Error cancelling request: " + e.getMessage()));
        }
    }

    private String extractBusinessMessage(String fullMessage) {
        if (fullMessage == null) {
            return "Operation failed";
        }
        if (fullMessage.contains("ERROR:")) {
            int errorIndex = fullMessage.indexOf("ERROR:");
            String afterError = fullMessage.substring(errorIndex + 6).trim();
            int newlineIndex = afterError.indexOf("\n");
            if (newlineIndex > 0) {
                return afterError.substring(0, newlineIndex).trim();
            }
            return afterError;
        }
        return fullMessage;
    }
}