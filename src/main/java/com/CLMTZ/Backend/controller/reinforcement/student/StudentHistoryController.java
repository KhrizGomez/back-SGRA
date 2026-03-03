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
                return ResponseEntity.badRequest().body(Map.of("message", "La página debe ser mayor o igual a 1"));
            }
            if (size < 1 || size > 100) {
                return ResponseEntity.badRequest().body(Map.of("message", "El tamaño debe estar entre 1 y 100"));
            }
            if (periodId != null && periodId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "ID de periodo inválido"));
            }
            if (statusId != null && statusId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "ID de estado inválido"));
            }

            StudentHistoryRequestsPageDTO response = studentHistoryService.getRequestHistory(userId, periodId, page,
                    size, statusId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Error al obtener el historial de solicitudes: " + e.getMessage()));
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
                return ResponseEntity.badRequest().body(Map.of("message", "La página debe ser mayor o igual a 1"));
            }
            if (size < 1 || size > 100) {
                return ResponseEntity.badRequest().body(Map.of("message", "El tamaño debe estar entre 1 y 100"));
            }

            StudentHistorySessionsPageDTO response = studentHistoryService.getPreviousSessions(userId, page, size,
                    onlyAttended);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Error al obtener las sesiones anteriores: " + e.getMessage()));
        }
    }
}