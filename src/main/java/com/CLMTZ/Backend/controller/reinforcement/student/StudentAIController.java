package com.CLMTZ.Backend.controller.reinforcement.student;

import com.CLMTZ.Backend.config.UserContextHolder;
import com.CLMTZ.Backend.dto.ai.ChatResponse;
import com.CLMTZ.Backend.dto.ai.StudentAISuggestRequestDTO;
import com.CLMTZ.Backend.dto.ai.StudentAISuggestionResponseDTO;
import com.CLMTZ.Backend.dto.security.session.UserContext;
import com.CLMTZ.Backend.service.reinforcement.student.StudentAIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/student/ai")
@RequiredArgsConstructor
public class StudentAIController {

    private final StudentAIService studentAIService;

    /**
     * Feature 5: FAQ académico guiado.
     * El estudiante pregunta sobre procesos internos del SGRA y recibe respuestas en lenguaje simple.
     * Body: { "message": "¿Cómo cancelo una solicitud?" }
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody Map<String, String> body) {
        UserContext ctx = UserContextHolder.getContext();
        String message = body.getOrDefault("message", "").trim();
        if (message.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ChatResponse.builder().success(false).error("El mensaje no puede estar vacío.").build());
        }
        return ResponseEntity.ok(studentAIService.chat(message, ctx.getUserId()));
    }

    /**
     * Feature 1: Asistente para crear solicitudes.
     * El estudiante describe su problema y la IA sugiere tipo de sesión, motivo redactado y evidencias.
     * Body: { "subjectId": 12, "subjectName": "Cálculo I", "problemDescription": "No entiendo integrales" }
     */
    @PostMapping("/suggest-request")
    public ResponseEntity<StudentAISuggestionResponseDTO> suggestRequest(
            @RequestBody StudentAISuggestRequestDTO request) {
        UserContext ctx = UserContextHolder.getContext();
        if (request.getProblemDescription() == null || request.getProblemDescription().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    StudentAISuggestionResponseDTO.builder()
                            .success(false).error("Describe el problema para obtener sugerencias.").build());
        }
        return ResponseEntity.ok(studentAIService.suggestRequest(request, ctx.getUserId()));
    }

    /**
     * Feature 2: Explicador de estado de solicitud.
     * Explica en lenguaje simple por qué la solicitud está en su estado actual.
     */
    @GetMapping("/explain-status/{requestId}")
    public ResponseEntity<ChatResponse> explainStatus(@PathVariable Integer requestId) {
        UserContext ctx = UserContextHolder.getContext();
        if (requestId == null || requestId <= 0) {
            return ResponseEntity.badRequest().body(
                    ChatResponse.builder().success(false).error("ID de solicitud inválido.").build());
        }
        return ResponseEntity.ok(studentAIService.explainStatus(requestId, ctx.getUserId()));
    }

    /**
     * Feature 3: Preparador de sesión de reforzamiento.
     * Genera un mini plan de estudio y checklist antes de la tutoría.
     */
    @GetMapping("/prep-session/{sessionId}")
    public ResponseEntity<ChatResponse> prepSession(@PathVariable Integer sessionId) {
        UserContext ctx = UserContextHolder.getContext();
        if (sessionId == null || sessionId <= 0) {
            return ResponseEntity.badRequest().body(
                    ChatResponse.builder().success(false).error("ID de sesión inválido.").build());
        }
        return ResponseEntity.ok(studentAIService.prepSession(sessionId, ctx.getUserId()));
    }

    /**
     * Feature 4: Resumen post-sesión.
     * Transforma las observaciones del docente en pasos accionables para el estudiante.
     */
    @GetMapping("/post-session/{sessionId}")
    public ResponseEntity<ChatResponse> postSession(@PathVariable Integer sessionId) {
        UserContext ctx = UserContextHolder.getContext();
        if (sessionId == null || sessionId <= 0) {
            return ResponseEntity.badRequest().body(
                    ChatResponse.builder().success(false).error("ID de sesión inválido.").build());
        }
        return ResponseEntity.ok(studentAIService.postSession(sessionId, ctx.getUserId()));
    }
}