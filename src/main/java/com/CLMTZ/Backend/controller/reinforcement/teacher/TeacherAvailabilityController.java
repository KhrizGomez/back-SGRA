package com.CLMTZ.Backend.controller.reinforcement.teacher;

import com.CLMTZ.Backend.config.UserContextHolder;
import com.CLMTZ.Backend.dto.reinforcement.TeacherAvailabilityBatchDTO;
import com.CLMTZ.Backend.dto.reinforcement.TeacherAvailabilitySlotDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherActionResponseDTO;
import com.CLMTZ.Backend.dto.security.session.UserContext;
import com.CLMTZ.Backend.service.reinforcement.teacher.TeacherAvailabilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Gestión de horarios de disponibilidad del docente.
 * GET  /api/teacher/availability?periodId=   → obtiene franjas configuradas
 * POST /api/teacher/availability             → guarda franjas (reemplaza el batch del periodo)
 */
@RestController
@RequestMapping("/api/teacher/availability")
public class TeacherAvailabilityController {

    private final TeacherAvailabilityService teacherAvailabilityService;

    public TeacherAvailabilityController(TeacherAvailabilityService teacherAvailabilityService) {
        this.teacherAvailabilityService = teacherAvailabilityService;
    }

    /**
     * Obtiene las franjas horarias configuradas por el docente para un periodo.
     */
    @GetMapping
    public ResponseEntity<?> getAvailability(@RequestParam("periodId") Integer periodId) {
        try {
            if (periodId == null || periodId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "periodId inválido"));
            }
            UserContext ctx = UserContextHolder.getContext();
            List<TeacherAvailabilitySlotDTO> slots =
                    teacherAvailabilityService.getMyAvailability(ctx.getUserId(), periodId);
            return ResponseEntity.ok(slots);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Error obteniendo disponibilidad: " + e.getMessage()));
        }
    }

    /**
     * Guarda (reemplaza) las franjas horarias del docente para un periodo.
     * Body: { "periodId": 1, "slots": [{ "dayOfWeek": 1, "timeSlotId": 3 }, ...] }
     */
    @PostMapping
    public ResponseEntity<?> saveAvailability(@RequestBody TeacherAvailabilityBatchDTO dto) {
        try {
            if (dto.getPeriodId() == null || dto.getPeriodId() <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "periodId es requerido"));
            }
            if (dto.getSlots() == null) {
                dto.setSlots(List.of());
            }
            UserContext ctx = UserContextHolder.getContext();
            TeacherActionResponseDTO response =
                    teacherAvailabilityService.saveMyAvailability(ctx.getUserId(), dto.getPeriodId(), dto.getSlots());

            if ("ERROR".equals(response.getStatus())) {
                return ResponseEntity.status(409).body(response);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Error guardando disponibilidad: " + e.getMessage()));
        }
    }
}