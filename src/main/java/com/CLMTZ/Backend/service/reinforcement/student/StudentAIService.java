package com.CLMTZ.Backend.service.reinforcement.student;

import com.CLMTZ.Backend.dto.ai.ChatResponse;
import com.CLMTZ.Backend.dto.ai.StudentAISuggestRequestDTO;
import com.CLMTZ.Backend.dto.ai.StudentAISuggestionResponseDTO;
import com.CLMTZ.Backend.repository.ai.StudentAIContextRepository;
import com.CLMTZ.Backend.service.ai.GroqAIService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentAIService {

    private final GroqAIService groqAIService;
    private final StudentAIContextRepository contextRepository;
    private final ObjectMapper objectMapper;

    // ─── Feature 5: FAQ académico guiado ─────────────────────────────────────

    public ChatResponse chat(String message, Integer userId) {
        try {
            String systemPrompt = loadPrompt("chat/estudiante.txt");
            if (systemPrompt == null) return errorResponse("Servicio de chat no configurado.");

            String context = contextRepository.getStudentContext(userId);
            String userPrompt = buildContextualPrompt(context, message);

            String raw = groqAIService.chatFreeText(systemPrompt, userPrompt);
            if (raw == null || raw.isBlank()) return errorResponse("El servicio de IA no respondió. Intenta nuevamente.");

            return ChatResponse.builder().response(raw.trim()).module("estudiante").success(true).build();

        } catch (Exception e) {
            log.error("[StudentAIService] chat error para userId={}: {}", userId, e.getMessage());
            return errorResponse("Error al procesar tu consulta. Intenta nuevamente.");
        }
    }

    // ─── Feature 1: Asistente para crear solicitudes ─────────────────────────

    /**
     * El estudiante describe su problema en texto libre y la IA devuelve:
     * - tipoSesion: "individual" o "grupal"
     * - motivoSugerido: redacción mejorada del motivo
     * - evidencias: lista de documentos/evidencias recomendadas
     * - razonamiento: por qué estas sugerencias
     */
    public StudentAISuggestionResponseDTO suggestRequest(StudentAISuggestRequestDTO req, Integer userId) {
        try {
            String systemPrompt = loadPrompt("student/request-suggester.txt");
            if (systemPrompt == null) {
                return StudentAISuggestionResponseDTO.builder()
                        .success(false).error("Servicio de sugerencias no configurado.").build();
            }

            String userPrompt = """
                    Asignatura: %s

                    Problema descrito por el estudiante:
                    %s
                    """.formatted(
                    req.getSubjectName() != null ? req.getSubjectName() : "No especificada",
                    req.getProblemDescription().trim()
            );

            // Usamos chat() que fuerza respuesta JSON
            String raw = groqAIService.chat(systemPrompt, userPrompt);
            if (raw == null || raw.isBlank()) {
                return StudentAISuggestionResponseDTO.builder()
                        .success(false).error("El servicio de IA no respondió. Intenta nuevamente.").build();
            }
            return parseSuggestionResponse(raw);

        } catch (Exception e) {
            log.error("[StudentAIService] suggestRequest error para userId={}: {}", userId, e.getMessage());
            return StudentAISuggestionResponseDTO.builder()
                    .success(false).error("Error al generar sugerencias. Intenta nuevamente.").build();
        }
    }

    // ─── Feature 2: Explicador de estado ─────────────────────────────────────

    /**
     * Explica en lenguaje simple por qué la solicitud está en su estado actual,
     * usando el historial real de estados de la BD.
     */
    public ChatResponse explainStatus(Integer requestId, Integer userId) {
        try {
            String systemPrompt = loadPrompt("student/status-explainer.txt");
            if (systemPrompt == null) return errorResponse("Servicio no configurado.");

            String context = contextRepository.getRequestContext(requestId, userId);
            if ("{}".equals(context)) {
                return errorResponse("No se encontró información de esta solicitud.");
            }

            String raw = groqAIService.chatFreeText(systemPrompt, context);
            if (raw == null || raw.isBlank()) return errorResponse("El servicio de IA no respondió. Intenta nuevamente.");

            return ChatResponse.builder().response(raw.trim()).module("estudiante").success(true).build();

        } catch (Exception e) {
            log.error("[StudentAIService] explainStatus error requestId={}: {}", requestId, e.getMessage());
            return errorResponse("Error al explicar el estado. Intenta nuevamente.");
        }
    }

    // ─── Feature 3: Preparador de sesión ─────────────────────────────────────

    /**
     * Genera un mini plan de estudio y checklist para que el estudiante llegue
     * preparado a su sesión de reforzamiento.
     */
    public ChatResponse prepSession(Integer sessionId, Integer userId) {
        try {
            String systemPrompt = loadPrompt("student/session-prep.txt");
            if (systemPrompt == null) return errorResponse("Servicio no configurado.");

            String context = contextRepository.getSessionContext(sessionId, userId);
            if ("{}".equals(context)) {
                return errorResponse("No se encontró información de esta sesión.");
            }

            String raw = groqAIService.chatFreeText(systemPrompt, context);
            if (raw == null || raw.isBlank()) return errorResponse("El servicio de IA no respondió. Intenta nuevamente.");

            return ChatResponse.builder().response(raw.trim()).module("estudiante").success(true).build();

        } catch (Exception e) {
            log.error("[StudentAIService] prepSession error sessionId={}: {}", sessionId, e.getMessage());
            return errorResponse("Error al preparar la sesión. Intenta nuevamente.");
        }
    }

    // ─── Feature 4: Resumen post-sesión ──────────────────────────────────────

    /**
     * Transforma las observaciones del docente en "siguientes pasos" concretos
     * y accionables para el estudiante.
     */
    public ChatResponse postSession(Integer sessionId, Integer userId) {
        try {
            String systemPrompt = loadPrompt("student/post-session.txt");
            if (systemPrompt == null) return errorResponse("Servicio no configurado.");

            String context = contextRepository.getSessionContext(sessionId, userId);
            if ("{}".equals(context)) {
                return errorResponse("No se encontró información de esta sesión.");
            }

            String raw = groqAIService.chatFreeText(systemPrompt, context);
            if (raw == null || raw.isBlank()) return errorResponse("El servicio de IA no respondió. Intenta nuevamente.");

            return ChatResponse.builder().response(raw.trim()).module("estudiante").success(true).build();

        } catch (Exception e) {
            log.error("[StudentAIService] postSession error sessionId={}: {}", sessionId, e.getMessage());
            return errorResponse("Error al generar el resumen. Intenta nuevamente.");
        }
    }

    // ─── Helpers privados ─────────────────────────────────────────────────────

    private String loadPrompt(String relativePath) {
        String fullPath = "prompts/" + relativePath;
        try {
            ClassPathResource resource = new ClassPathResource(fullPath);
            try (InputStream is = resource.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.error("[StudentAIService] No se encontró prompt: {}", fullPath);
            return null;
        }
    }

    private String buildContextualPrompt(String contextJson, String message) {
        return """
                CONTEXTO DEL ESTUDIANTE (datos reales de la base de datos):
                %s

                PREGUNTA DEL ESTUDIANTE:
                %s
                """.formatted(contextJson, message);
    }

    private StudentAISuggestionResponseDTO parseSuggestionResponse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);

            List<String> evidencias = new ArrayList<>();
            JsonNode evNode = root.get("evidencias");
            if (evNode != null && evNode.isArray()) {
                evNode.forEach(e -> evidencias.add(e.asText()));
            }

            return StudentAISuggestionResponseDTO.builder()
                    .tipoSesion(root.path("tipoSesion").asText("individual"))
                    .motivoSugerido(root.path("motivoSugerido").asText(""))
                    .evidencias(evidencias)
                    .razonamiento(root.path("razonamiento").asText(""))
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("[StudentAIService] Error parseando respuesta JSON de sugerencia: {}", e.getMessage());
            return StudentAISuggestionResponseDTO.builder()
                    .success(false).error("No se pudo interpretar la respuesta de la IA.").build();
        }
    }

    private ChatResponse errorResponse(String message) {
        return ChatResponse.builder().success(false).error(message).build();
    }
}