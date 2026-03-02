package com.CLMTZ.Backend.controller.reinforcement.student;

import com.CLMTZ.Backend.config.UserContextHolder;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentHistoryRequestsPageDTO;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentHistorySessionsPageDTO;
import com.CLMTZ.Backend.dto.security.session.UserContext;
import com.CLMTZ.Backend.service.reinforcement.student.StudentHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/student/history")
public class StudentHistoryController {

    private final StudentHistoryService studentHistoryService;

    public StudentHistoryController(StudentHistoryService studentHistoryService) {
        this.studentHistoryService = studentHistoryService;
    }

    @GetMapping("/requests")
    public ResponseEntity<?> getRequestHistory(
            @RequestParam(value = "periodId", required = false) Integer periodId,
            @RequestParam(value = "statusId", required = false) Integer statusId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        try {
            UserContext ctx = UserContextHolder.getContext();
            Integer userId = ctx.getUserId();

            if (page < 1) {
                return ResponseEntity.badRequest().body(Map.of("message", "Page must be greater than or equal to 1"));
            }
            if (size < 1 || size > 100) {
                return ResponseEntity.badRequest().body(Map.of("message", "Size must be between 1 and 100"));
            }
            if (periodId != null && periodId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid periodId parameter"));
            }
            if (statusId != null && statusId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid statusId parameter"));
            }

            StudentHistoryRequestsPageDTO response = studentHistoryService.getRequestHistory(userId, periodId, page,
                    size, statusId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Error retrieving request history: " + e.getMessage()));
        }
    }

    @GetMapping("/sessions")
    public ResponseEntity<?> getPreviousSessions(
            @RequestParam(value = "onlyAttended", defaultValue = "false") Boolean onlyAttended,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        try {
            UserContext ctx = UserContextHolder.getContext();
            Integer userId = ctx.getUserId();

            if (page < 1) {
                return ResponseEntity.badRequest().body(Map.of("message", "Page must be greater than or equal to 1"));
            }
            if (size < 1 || size > 100) {
                return ResponseEntity.badRequest().body(Map.of("message", "Size must be between 1 and 100"));
            }

            StudentHistorySessionsPageDTO response = studentHistoryService.getPreviousSessions(userId, page, size,
                    onlyAttended);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Error retrieving previous sessions: " + e.getMessage()));
        }
    }
}