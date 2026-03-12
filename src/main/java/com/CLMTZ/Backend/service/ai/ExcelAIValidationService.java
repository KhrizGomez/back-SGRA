package com.CLMTZ.Backend.service.ai;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.CLMTZ.Backend.dto.ai.AIValidationIssue;
import com.CLMTZ.Backend.dto.ai.AIValidationRequest;
import com.CLMTZ.Backend.dto.ai.AIValidationResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelAIValidationService {

    private final GroqAIService groqAIService;
    private final JavaFallbackValidator fallbackValidator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Valida los datos del Excel usando IA (Groq) con fallback a reglas Java.
     */
    public AIValidationResult validate(AIValidationRequest request) {
        log.info("[ExcelAIValidationService] Iniciando validación para tipo: {} con {} filas",
                request.getLoadType(), request.getRows().size());

        // Intentar validación con IA primero
        if (groqAIService.isAvailable()) {
            try {
                AIValidationResult aiResult = validateWithAI(request);
                if (aiResult != null) {
                    return aiResult;
                }
            } catch (Exception e) {
                log.warn("[ExcelAIValidationService] Groq AI falló, usando fallback Java. Error: {}", e.getMessage());
            }
        } else {
            log.info("[ExcelAIValidationService] Groq AI no disponible (sin API key), usando fallback Java.");
        }

        // Fallback a validaciones Java
        return fallbackValidator.validate(
                request.getRows(),
                request.getLoadType(),
                getRequiredFieldsForType(request.getLoadType())
        );
    }

    /**
     * Intenta validar los datos usando la API de Groq.
    */
    private AIValidationResult validateWithAI(AIValidationRequest request) {
        long startTime = System.currentTimeMillis();

        // 1. Cargar template de prompt
        String systemPrompt = loadPromptTemplate(request.getLoadType());
        if (systemPrompt == null || systemPrompt.isBlank()) {
            log.warn("[ExcelAIValidationService] No se encontró template para tipo: {}", request.getLoadType());
            return null;
        }

        // 2. Construir prompt del usuario con los datos
        String userPrompt = buildUserPrompt(request);
        if (userPrompt.isBlank()) {
            log.warn("[ExcelAIValidationService] No se pudieron serializar los datos para IA");
            return null;
        }

        // 3. Llamar a la API de Groq
        String aiResponse = groqAIService.chat(systemPrompt, userPrompt);
        if (aiResponse == null || aiResponse.isBlank()) {
            log.warn("[ExcelAIValidationService] Respuesta vacía de Groq AI");
            return null;
        }

        // 4. Parsear respuesta JSON
        AIValidationResult result = parseAIResponse(aiResponse);
        if (result != null) {
            result.setAiValidated(true);
            result.setValidationTimeMs(System.currentTimeMillis() - startTime);
        }

        return result;
    }

    /**
     * Carga la plantilla de prompt desde resources/prompts/{loadType}.txt
     *
     * @param loadType tipo de carga
     * @return contenido del prompt template
     */
    protected String loadPromptTemplate(String loadType) {
        String path = "prompts/" + loadType + ".txt";
        try {
            ClassPathResource resource = new ClassPathResource(path);
            InputStream is = resource.getInputStream();
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("[ExcelAIValidationService] No se encontró template de prompt: {}", path);
            return null;
        }
    }

    /**
     * Construye el prompt del usuario con los datos del Excel serializados.
     */
    protected String buildUserPrompt(AIValidationRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Analiza los siguientes datos extraídos de un archivo Excel.\n");
        sb.append("Tipo de carga: ").append(request.getLoadType()).append("\n");
        sb.append("Total de filas: ").append(request.getRows().size()).append("\n\n");
        sb.append("DATOS A VALIDAR (formato JSON):\n");

        try {
            // Limitar a las primeras 100 filas para no exceder tokens
            List<Map<String, Object>> rowsToValidate = request.getRows();
            if (rowsToValidate.size() > 100) {
                rowsToValidate = rowsToValidate.subList(0, 100);
                sb.append("(NOTA: Mostrando primeras 100 filas de ").append(request.getRows().size()).append(" totales)\n");
            }

            String jsonData = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rowsToValidate);
            sb.append(jsonData);
        } catch (Exception e) {
            log.error("[ExcelAIValidationService] Error al serializar datos: {}", e.getMessage());
            return "";
        }

        return sb.toString();
    }

    /**
     * Parsea la respuesta JSON del modelo IA a un AIValidationResult.
     *
     * @param aiResponse respuesta cruda del modelo
     * @return resultado estructurado
     */
    protected AIValidationResult parseAIResponse(String aiResponse) {
        try {
            JsonNode root = objectMapper.readTree(aiResponse);

            List<AIValidationIssue> issues = new ArrayList<>();
            JsonNode issuesNode = root.get("issues");
            if (issuesNode != null && issuesNode.isArray()) {
                for (JsonNode issueNode : issuesNode) {
                    AIValidationIssue issue = AIValidationIssue.builder()
                            .row(issueNode.has("row") ? issueNode.get("row").asInt() : 0)
                            .field(issueNode.has("field") ? issueNode.get("field").asText() : "")
                            .severity(parseSeverity(issueNode.has("severity") ? issueNode.get("severity").asText() : "WARNING"))
                            .message(issueNode.has("message") ? issueNode.get("message").asText() : "")
                            .suggestion(issueNode.has("suggestion") ? issueNode.get("suggestion").asText() : null)
                            .source("AI")
                            .build();
                    issues.add(issue);
                }
            }

            String recommendedAction = root.has("recommendedAction") ? root.get("recommendedAction").asText() : "REVIEW";
            String summary = root.has("summary") ? root.get("summary").asText() : "Validación completada por IA";

            return AIValidationResult.builder()
                    .issues(issues)
                    .aiValidated(true)
                    .recommendedAction(recommendedAction)
                    .summary(summary)
                    .build();

        } catch (Exception e) {
            log.error("[ExcelAIValidationService] Error al parsear respuesta IA: {}", e.getMessage());
            return null;
        }
    }

    private AIValidationIssue.Severity parseSeverity(String severity) {
        return switch (severity.toUpperCase()) {
            case "ERROR" -> AIValidationIssue.Severity.ERROR;
            case "INFO" -> AIValidationIssue.Severity.INFO;
            default -> AIValidationIssue.Severity.WARNING;
        };
    }

    /**
     * Retorna los campos obligatorios según el tipo de carga.
     * Se usa tanto para el prompt de IA como para el fallback Java.
     *
     * @param loadType tipo de carga
     * @return lista de nombres de campos obligatorios
     */
    protected List<String> getRequiredFieldsForType(String loadType) {
        return switch (loadType) {
            // Estudiantes.xls: campos presentes en excelToGenericMap
            case "students" -> List.of("nombre_completo", "identificacion", "correo", "telefono1");
            // Docente.xls: campos presentes en excelToGenericMap
            case "teachers" -> List.of("coordinacion", "carrera", "nivel", "materia", "paralelo", "profesor");
            case "class_schedules" -> List.of("cedulaDocente", "nombreAsignatura", "nombreParalelo", "nombrePeriodo", "diaSemana", "horaInicio", "horaFin");
            case "careers" -> List.of("nombre", "codigo");
            case "subjects" -> List.of("nombre", "codigo");
            // Matricula.xlsx: campos presentes en excelToGenericMap
            case "registrations" -> List.of("identificacion", "apellidos", "nombres", "sexo");
            default -> List.of();
        };
    }

    /**
     * Retorna las reglas de negocio específicas por tipo de carga.
     * Se usan para enriquecer el prompt de IA.
     *
     * @param loadType tipo de carga
     * @return lista de reglas en lenguaje natural
     */
    protected List<String> getBusinessRulesForType(String loadType) {
        return switch (loadType) {
            case "students" -> List.of(
                    "La identificacion debe ser cedula ecuatoriana valida (10 digitos) o pasaporte alfanumerico (5-20)",
                    "El correo debe ser un email valido",
                    "No pueden existir dos estudiantes con la misma identificacion en el mismo archivo"
            );
            case "class_schedules" -> List.of(
                    "El dia de la semana debe estar entre 1 (Lunes) y 7 (Domingo)",
                    "La hora de inicio debe ser anterior a la hora de fin",
                    "No pueden existir horarios duplicados (mismo docente, dia y hora)",
                    "La cedula del docente debe tener 10 digitos"
            );
            case "teachers" -> List.of(
                    "El profesor debe tener apellidos y nombres completos",
                    "La combinacion profesor + materia + paralelo no debe duplicarse en el archivo"
            );
            case "registrations" -> List.of(
                    "La identificacion debe ser cedula ecuatoriana valida (10 digitos) o pasaporte alfanumerico (5-20)",
                    "El sexo debe iniciar con M o F"
            );
            default -> List.of();
        };
    }
}



