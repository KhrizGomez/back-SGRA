package com.CLMTZ.Backend.service.ai;

import com.CLMTZ.Backend.config.UserContextHolder;
import com.CLMTZ.Backend.dto.ai.ChatRequest;
import com.CLMTZ.Backend.dto.ai.ChatResponse;
import com.CLMTZ.Backend.repository.ai.AIChatContextRepository;
import com.CLMTZ.Backend.repository.ai.StudentAIContextRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIChatService {

    private final GroqAIService groqAIService;
    private final AIChatContextRepository chatContextRepository;
    private final StudentAIContextRepository studentContextRepository;

    public ChatResponse chat(ChatRequest request) {
        try {
            // 1. Cargar system prompt del módulo desde resources/prompts/chat/{module}.txt
            String systemPrompt = loadSystemPrompt(request.getModule());
            if (systemPrompt == null) {
                return ChatResponse.builder()
                        .success(false)
                        .error("Módulo de chat no configurado: " + request.getModule())
                        .build();
            }

            // 2. Obtener contexto de BD según el módulo
            String dbContextJson;
            try {
                dbContextJson = getDbContext(request.getModule());
            } catch (Exception e) {
                log.error("[AIChatService] Error al obtener contexto de BD para módulo '{}': {}", request.getModule(), e.getMessage());
                return ChatResponse.builder()
                        .success(false)
                        .error("No se pudo obtener la información del sistema. Verifica que las funciones de base de datos estén creadas e intenta nuevamente.")
                        .build();
            }

            // 3. Construir user prompt: contexto + pregunta del usuario
            String userPrompt = buildUserPrompt(dbContextJson, request.getMessage());

            // 4. Llamar a Groq en modo texto libre (no JSON)
            String rawResponse = groqAIService.chatFreeText(systemPrompt, userPrompt);
            if (rawResponse == null || rawResponse.isBlank()) {
                return ChatResponse.builder()
                        .success(false)
                        .error("El servicio de IA no respondió. Intenta nuevamente.")
                        .build();
            }

            return ChatResponse.builder()
                    .response(rawResponse.trim())
                    .module(request.getModule())
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("[AIChatService] Error procesando chat para módulo '{}': {}", request.getModule(), e.getMessage());
            return ChatResponse.builder()
                    .success(false)
                    .error("Error al procesar tu consulta. Intenta nuevamente.")
                    .build();
        }
    }

    private String loadSystemPrompt(String module) {
        String path = "prompts/chat/" + module + ".txt";
        try {
            ClassPathResource resource = new ClassPathResource(path);
            try (InputStream is = resource.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.error("[AIChatService] No se encontró prompt para módulo '{}' en {}", module, path);
            return null;
        }
    }

    private String getDbContext(String module) {
        return switch (module) {
            case "coordinacion" -> chatContextRepository.getCoordinacionContext();
            case "estudiante"   -> {
                try {
                    Integer userId = UserContextHolder.getContext().getUserId();
                    yield studentContextRepository.getStudentContext(userId);
                } catch (Exception e) {
                    log.warn("[AIChatService] No se pudo obtener contexto de estudiante: {}", e.getMessage());
                    yield "{}";
                }
            }
            case "docente" -> {
                try {
                    Integer userId = UserContextHolder.getContext().getUserId();
                    yield chatContextRepository.getDocenteContext(userId);
                } catch (Exception e) {
                    log.warn("[AIChatService] No se pudo obtener contexto de docente: {}", e.getMessage());
                    yield "{}";
                }
            }
            default -> "{}";
        };
    }

    private String buildUserPrompt(String contextJson, String userMessage) {
        return """
                CONTEXTO ACTUAL DEL SISTEMA (datos reales de la base de datos):
                %s

                PREGUNTA DEL USUARIO:
                %s
                """.formatted(contextJson, userMessage);
    }
}
