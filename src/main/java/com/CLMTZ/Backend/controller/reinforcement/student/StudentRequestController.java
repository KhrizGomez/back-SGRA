package com.CLMTZ.Backend.controller.reinforcement.student;

import com.CLMTZ.Backend.config.UserContextHolder;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentRequestCreateRequestDTO;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentRequestCreateResponseDTO;
import com.CLMTZ.Backend.dto.security.session.UserContext;
import com.CLMTZ.Backend.service.reinforcement.student.StudentRequestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/student/requests")
public class StudentRequestController {

    private final StudentRequestService studentRequestService;
    private final ObjectMapper objectMapper;

    public StudentRequestController(StudentRequestService studentRequestService, ObjectMapper objectMapper) {
        this.studentRequestService = studentRequestService;
        this.objectMapper = objectMapper;
    }

    /**
     * Crea una nueva solicitud de refuerzo.
     * Recibe los datos como multipart/form-data:
     * - "request": JSON con los datos de la solicitud (StudentRequestCreateRequestDTO)
     * - "files": archivos opcionales a adjuntar (MultipartFile[])
     */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> create(
            @RequestPart("request") String requestJson,
            @RequestPart(value = "files", required = false) MultipartFile[] files) {
        try {
            UserContext ctx = UserContextHolder.getContext();
            Integer userId = ctx.getUserId();

            // Deserializar el JSON del request
            StudentRequestCreateRequestDTO req = objectMapper.readValue(requestJson, StudentRequestCreateRequestDTO.class);

            // Validaciones
            if (req.getSubjectId() == null || req.getSubjectId() <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Debe seleccionar una asignatura"));
            }
            if (req.getSessionTypeId() == null || req.getSessionTypeId() <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Debe seleccionar un tipo de sesión"));
            }
            if (req.getReason() == null || req.getReason().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "El motivo no puede estar vacío"));
            }
            if (req.getReason().trim().length() < 10) {
                return ResponseEntity.badRequest().body(Map.of("message", "El motivo debe tener al menos 10 caracteres"));
            }

            StudentRequestCreateResponseDTO response = studentRequestService.create(req, userId, files);
            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            String msg = extractBusinessMessage(e.getMessage());
            // Detectar error de solicitud duplicada desde la BD
            if (msg.toLowerCase().contains("ya existe una solicitud")) {
                return ResponseEntity.status(409).body(Map.of("message", msg));
            }
            return ResponseEntity.status(500).body(Map.of("message", "Error al crear la solicitud: " + msg));
        }
    }

    private String extractBusinessMessage(String fullMessage) {
        if (fullMessage == null) {
            return "Error desconocido";
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