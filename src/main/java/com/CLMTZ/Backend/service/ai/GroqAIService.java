package com.CLMTZ.Backend.service.ai;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.CLMTZ.Backend.config.GroqProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroqAIService {

    private final GroqProperties groqProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Envía un prompt al modelo Groq y retorna la respuesta como String.
     */
    public String chat(String systemPrompt, String userPrompt) {
        if (!isAvailable()) {
            log.warn("[GroqAIService] API key no configurada, saltando validación IA");
            return null;
        }

        try {
            Map<String, Object> payload = buildRequestPayload(systemPrompt, userPrompt);
            String payloadJson = objectMapper.writeValueAsString(payload);

            log.info("[GroqAIService] Enviando request a Groq API...");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(groqProperties.getApi().getKey());

            HttpEntity<String> entity = new HttpEntity<>(payloadJson, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    groqProperties.getApi().getUrl(),
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            String responseBody = response.getBody();

            // Parsear respuesta y extraer choices[0].message.content
            JsonNode responseNode = objectMapper.readTree(responseBody);
            JsonNode choicesNode = responseNode.get("choices");
            if (choicesNode != null && choicesNode.isArray() && !choicesNode.isEmpty()) {
                JsonNode messageNode = choicesNode.get(0).get("message");
                if (messageNode != null) {
                    String content = messageNode.get("content").asText();
                    log.info("[GroqAIService] Respuesta recibida exitosamente");
                    return content;
                }
            }

            log.error("[GroqAIService] Respuesta de Groq sin contenido válido: {}", responseBody);
            return null;

        } catch (HttpClientErrorException e) {
            log.error("[GroqAIService] Error HTTP de Groq: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new GroqAIException("Error de API Groq: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("[GroqAIService] Error al comunicarse con Groq: {}", e.getMessage());
            throw new GroqAIException("Error de comunicación con Groq: " + e.getMessage(), e);
        }
    }

    /**
     * Verifica si el servicio de Groq está configurado y disponible.
     *
     * @return true si la API key está configurada y no está vacía
     */
    public boolean isAvailable() {
        String key = groqProperties.getApi().getKey();
        return key != null && !key.isBlank();
    }

    /**
     * Construye el payload JSON para la API de Groq.
     */
    protected Map<String, Object> buildRequestPayload(String systemPrompt, String userPrompt) {
        return Map.of(
                "model", groqProperties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "max_tokens", groqProperties.getMaxTokens(),
                "temperature", groqProperties.getTemperature(),
                "response_format", Map.of("type", "json_object")
        );
    }
    /**
     * Excepción lanzada cuando la API de Groq falla.
     */
    public static class GroqAIException extends RuntimeException {
        public GroqAIException(String message) {
            super(message);
        }

        public GroqAIException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

