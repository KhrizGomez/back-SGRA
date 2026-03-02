package com.CLMTZ.Backend.controller.reinforcement.teacher;

import com.CLMTZ.Backend.config.UserContextHolder;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherCancelSessionDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherRejectRequestDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherScheduleRequestDTO;
import com.CLMTZ.Backend.dto.security.session.UserContext;
import com.CLMTZ.Backend.service.reinforcement.teacher.TeacherRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Teacher request management controller.
 * RF10: View/Accept/Reject requests
 * RF11: Schedule session date, time slot and modality when accepting
 * RF15: Cancel an accepted session
 */
@RestController
@RequestMapping("/api/teacher/requests")
public class TeacherRequestController {

    private final TeacherRequestService teacherRequestService;

    public TeacherRequestController(TeacherRequestService teacherRequestService) {
        this.teacherRequestService = teacherRequestService;
    }

    /**
     * RF10: List incoming reinforcement requests for the authenticated teacher.
     * Optional filter by statusId and pagination.
     */
    @GetMapping
    public ResponseEntity<?> getIncomingRequests(
            @RequestParam(value = "statusId", required = false) Integer statusId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        try {
            UserContext ctx = UserContextHolder.getContext();
            Integer userId = ctx.getUserId();

            if (page < 1) page = 1;
            if (size < 1 || size > 100) size = 10;

            return ResponseEntity.ok(
                    teacherRequestService.getIncomingRequests(userId, statusId, page, size));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Error retrieving requests: " + extractBusinessMessage(e.getMessage())));
        }
    }

    /**
     * RF10 + RF11: Accept a pending request and schedule the session.
     * Body: { scheduledDate, timeSlotId, modalityId, estimatedDuration, reason, workAreaId (optional) }
     */
    @PutMapping("/{requestId}/accept")
    public ResponseEntity<?> acceptRequest(
            @PathVariable("requestId") Integer requestId,
            @RequestBody TeacherScheduleRequestDTO dto) {
        try {
            UserContext ctx = UserContextHolder.getContext();
            Integer userId = ctx.getUserId();

            if (requestId == null || requestId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid requestId"));
            }
            if (dto.getScheduledDate() == null || dto.getTimeSlotId() == null
                    || dto.getModalityId() == null || dto.getEstimatedDuration() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "scheduledDate, timeSlotId, modalityId y estimatedDuration son requeridos"));
            }

            var response = teacherRequestService.acceptRequest(userId, requestId, dto);

            if ("ERROR".equals(response.getStatus())) {
                return ResponseEntity.status(409).body(response);
            }
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            String msg = extractBusinessMessage(e.getMessage());
            return ResponseEntity.status(500).body(Map.of("message", "Error aceptando solicitud: " + msg));
        }
    }

    /**
     * RF10: Reject a pending request with an optional reason.
     * Body: { reason }
     */
    @PutMapping("/{requestId}/reject")
    public ResponseEntity<?> rejectRequest(
            @PathVariable("requestId") Integer requestId,
            @RequestBody(required = false) TeacherRejectRequestDTO dto) {
        try {
            UserContext ctx = UserContextHolder.getContext();
            Integer userId = ctx.getUserId();

            if (requestId == null || requestId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid requestId"));
            }

            String reason = dto != null ? dto.getReason() : null;
            var response = teacherRequestService.rejectRequest(userId, requestId, reason);

            if ("ERROR".equals(response.getStatus())) {
                return ResponseEntity.status(409).body(response);
            }
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            String msg = extractBusinessMessage(e.getMessage());
            return ResponseEntity.status(500).body(Map.of("message", "Error rechazando solicitud: " + msg));
        }
    }

    /**
     * RF11: Reschedule an already-accepted session.
     * Only available when status is "Aceptada".
     * Body: { scheduledDate, timeSlotId, modalityId, estimatedDuration, reason, workAreaId (optional) }
     */
    @PutMapping("/{requestId}/reschedule")
    public ResponseEntity<?> rescheduleRequest(
            @PathVariable("requestId") Integer requestId,
            @RequestBody TeacherScheduleRequestDTO dto) {
        try {
            UserContext ctx = UserContextHolder.getContext();
            Integer userId = ctx.getUserId();

            if (requestId == null || requestId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid requestId"));
            }
            if (dto.getScheduledDate() == null || dto.getTimeSlotId() == null
                    || dto.getModalityId() == null || dto.getEstimatedDuration() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "scheduledDate, timeSlotId, modalityId y estimatedDuration son requeridos"));
            }

            var response = teacherRequestService.rescheduleRequest(userId, requestId, dto);

            if ("ERROR".equals(response.getStatus())) {
                return ResponseEntity.status(409).body(response);
            }
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            String msg = extractBusinessMessage(e.getMessage());
            return ResponseEntity.status(500).body(Map.of("message", "Error reprogramando sesión: " + msg));
        }
    }

    /**
     * RF15: Cancel an accepted/scheduled session.
     * Body: { reason } (optional)
     */
    @PutMapping("/{scheduledId}/cancel")
    public ResponseEntity<?> cancelSession(
            @PathVariable("scheduledId") Integer scheduledId,
            @RequestBody(required = false) TeacherCancelSessionDTO dto) {
        try {
            UserContext ctx = UserContextHolder.getContext();
            Integer userId = ctx.getUserId();

            if (scheduledId == null || scheduledId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid scheduledId"));
            }

            String reason = dto != null ? dto.getReason() : null;
            var response = teacherRequestService.cancelSession(userId, scheduledId, reason);

            if ("ERROR".equals(response.getStatus())) {
                return ResponseEntity.status(409).body(response);
            }
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            String msg = extractBusinessMessage(e.getMessage());
            return ResponseEntity.status(500).body(Map.of("message", "Error cancelando sesión: " + msg));
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
