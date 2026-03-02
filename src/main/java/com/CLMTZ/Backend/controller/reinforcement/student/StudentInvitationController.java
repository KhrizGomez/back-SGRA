package com.CLMTZ.Backend.controller.reinforcement.student;

import com.CLMTZ.Backend.dto.reinforcement.student.StudentInvitationItemDTO;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentInvitationResponseDTO;
import com.CLMTZ.Backend.service.reinforcement.student.StudentInvitationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller para las invitaciones a tutorías grupales del estudiante.
 * Permite listar invitaciones pendientes y aceptar/rechazar.
 */
@RestController
@RequestMapping("/api/student/invitations")
public class StudentInvitationController {

    private final StudentInvitationService studentInvitationService;

    public StudentInvitationController(StudentInvitationService studentInvitationService) {
        this.studentInvitationService = studentInvitationService;
    }

    /**
     * Lista las invitaciones a tutorías grupales pendientes de respuesta.
     */
    @GetMapping
    public ResponseEntity<?> getPendingInvitations() {
        try {
            List<StudentInvitationItemDTO> invitations = studentInvitationService.getPendingInvitations();
            return ResponseEntity.ok(invitations);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Error al obtener invitaciones: " + e.getMessage()));
        }
    }

    /**
     * Acepta o rechaza una invitación a tutoría grupal.
     * Body esperado: { "accept": true/false }
     */
    @PutMapping("/{participantId}")
    public ResponseEntity<?> respondInvitation(
            @PathVariable("participantId") Integer participantId,
            @RequestBody Map<String, Boolean> body) {
        try {
            if (participantId == null || participantId <= 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "ID de invitación inválido"));
            }

            Boolean accept = body.get("accept");
            if (accept == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Debe indicar si acepta o rechaza la invitación"));
            }

            StudentInvitationResponseDTO response = studentInvitationService.respondInvitation(participantId, accept);

            if (response.getSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Error al responder invitación: " + e.getMessage()));
        }
    }
}

