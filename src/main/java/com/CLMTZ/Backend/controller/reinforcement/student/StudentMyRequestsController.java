package com.CLMTZ.Backend.controller.reinforcement.student;

import com.CLMTZ.Backend.config.UserContextHolder;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentMyRequestsChipsDTO;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentMyRequestsPageDTO;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentRequestResourcesDTO;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentMyRequestsStatusSummaryDTO;
import com.CLMTZ.Backend.dto.security.session.UserContext;
import com.CLMTZ.Backend.service.reinforcement.student.StudentMyRequestsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student/requests")
public class StudentMyRequestsController {

    private final StudentMyRequestsService studentMyRequestsService;

    public StudentMyRequestsController(StudentMyRequestsService studentMyRequestsService) {
        this.studentMyRequestsService = studentMyRequestsService;
    }

    @GetMapping
    public ResponseEntity<?> getMyRequests(
            @RequestParam(value = "periodId", required = false) Integer periodId,
            @RequestParam(value = "statusId", required = false) Integer statusId,
            @RequestParam(value = "sessionTypeId", required = false) Integer sessionTypeId,
            @RequestParam(value = "subjectId", required = false) Integer subjectId,
            @RequestParam(value = "search", required = false) String search,
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
            if (sessionTypeId != null && sessionTypeId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "ID de tipo de sesión inválido"));
            }
            if (subjectId != null && subjectId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "ID de asignatura inválido"));
            }

            StudentMyRequestsPageDTO response = studentMyRequestsService.getMyRequests(
                    userId, periodId, statusId, sessionTypeId, subjectId, search, page, size);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error al obtener solicitudes: " + e.getMessage()));
        }
    }

    @GetMapping("/chips")
    public ResponseEntity<?> getMyRequestsChips(@RequestParam(value = "periodId", required = false) Integer periodId) {
        try {
            UserContext ctx = UserContextHolder.getContext();
            Integer userId = ctx.getUserId();

            if (periodId != null && periodId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "ID de periodo inválido"));
            }

            StudentMyRequestsChipsDTO response = studentMyRequestsService.getMyRequestsChips(userId, periodId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error al obtener resumen: " + e.getMessage()));
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getMyRequestsSummary(
            @RequestParam(value = "periodId", required = false) Integer periodId) {
        try {
            UserContext ctx = UserContextHolder.getContext();
            Integer userId = ctx.getUserId();

            if (periodId != null && periodId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "ID de periodo inválido"));
            }

            List<StudentMyRequestsStatusSummaryDTO> response = studentMyRequestsService.getMyRequestsSummary(userId,
                    periodId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error al obtener resumen de estados: " + e.getMessage()));
        }
    }

    @GetMapping("/{requestId}/resources")
    public ResponseEntity<?> getRequestResources(@PathVariable("requestId") Integer requestId) {
        try {
            UserContext ctx = UserContextHolder.getContext();
            Integer userId = ctx.getUserId();

            if (requestId == null || requestId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "ID de solicitud inválido"));
            }

            StudentRequestResourcesDTO response = studentMyRequestsService.getRequestResources(userId, requestId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error al obtener recursos: " + e.getMessage()));
        }
    }
}