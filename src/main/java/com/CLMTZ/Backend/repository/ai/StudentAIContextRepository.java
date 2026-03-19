package com.CLMTZ.Backend.repository.ai;

import com.CLMTZ.Backend.config.DynamicDataSourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class StudentAIContextRepository {

    private final DynamicDataSourceService dynamicDataSourceService;

    private NamedParameterJdbcTemplate jdbc() {
        return dynamicDataSourceService.getJdbcTemplate();
    }

    /**
     * Contexto general del estudiante para el chat FAQ.
     * Incluye solicitudes activas, sesiones próximas e historial del período activo.
     */
    public String getStudentContext(Integer userId) {
        try {
            MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
            String result = jdbc().queryForObject(
                    "SELECT reforzamiento.fn_get_estudiante_chat_context(:userId)::text",
                    params, String.class);
            return result != null ? result : "{}";
        } catch (Exception e) {
            log.warn("[StudentAIContextRepository] fn_get_estudiante_chat_context no disponible (userId={}): {}",
                    userId, e.getMessage());
            return "{}";
        }
    }

    /**
     * Contexto de una solicitud específica para explicar su estado.
     * Valida que la solicitud pertenezca al usuario antes de retornar datos.
     */
    public String getRequestContext(Integer requestId, Integer userId) {
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("requestId", requestId)
                    .addValue("userId", userId);
            String result = jdbc().queryForObject(
                    "SELECT reforzamiento.fn_get_estudiante_request_context(:requestId, :userId)::text",
                    params, String.class);
            return result != null ? result : "{}";
        } catch (Exception e) {
            log.warn("[StudentAIContextRepository] fn_get_estudiante_request_context no disponible (requestId={}): {}",
                    requestId, e.getMessage());
            return "{}";
        }
    }

    /**
     * Contexto de una sesión programada, usado tanto para prepararla (Feature 3)
     * como para el resumen post-sesión (Feature 4).
     * Valida que el estudiante sea participante de la sesión.
     */
    public String getSessionContext(Integer sessionId, Integer userId) {
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("sessionId", sessionId)
                    .addValue("userId", userId);
            String result = jdbc().queryForObject(
                    "SELECT reforzamiento.fn_get_estudiante_session_context(:sessionId, :userId)::text",
                    params, String.class);
            return result != null ? result : "{}";
        } catch (Exception e) {
            log.warn("[StudentAIContextRepository] fn_get_estudiante_session_context no disponible (sessionId={}): {}",
                    sessionId, e.getMessage());
            return "{}";
        }
    }
}