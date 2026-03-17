package com.CLMTZ.Backend.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Respuesta del asistente de creación de solicitudes (Feature 1).
 * La IA devuelve sugerencias estructuradas que el frontend puede pre-llenar en el formulario.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAISuggestionResponseDTO {

    /** Tipo de sesión sugerida: "individual" o "grupal" */
    private String tipoSesion;

    /** Redacción mejorada del motivo de la solicitud, lista para copiar al formulario */
    private String motivoSugerido;

    /** Lista de evidencias o documentos que el estudiante debería adjuntar */
    private List<String> evidencias;

    /** Explicación breve de por qué la IA hizo estas sugerencias */
    private String razonamiento;

    private boolean success;
    private String error;
}