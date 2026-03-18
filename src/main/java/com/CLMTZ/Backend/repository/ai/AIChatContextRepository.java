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
public class AIChatContextRepository {

    private final DynamicDataSourceService dynamicDataSourceService;

    private NamedParameterJdbcTemplate jdbc() {
        return dynamicDataSourceService.getJdbcTemplate();
    }

    public String getCoordinacionContext() {
        Integer periodId = jdbc().getJdbcTemplate()
                .queryForObject("SELECT academico.fn_sl_id_periodo_activo()", Integer.class);
        if (periodId == null) return "{}";

        MapSqlParameterSource params = new MapSqlParameterSource("periodId", periodId);
        return jdbc().queryForObject(
                "SELECT reforzamiento.fn_get_coordinacion_chat_context(:periodId)::text",
                params,
                String.class
        );
    }

    public String getDocenteContext(Integer userId) {
        try {
            MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
            String result = jdbc().queryForObject(
                    "SELECT reforzamiento.fn_get_docente_chat_context(:userId)::text",
                    params,
                    String.class
            );
            return result != null ? result : "{}";
        } catch (Exception e) {
            log.warn("[AIChatContextRepository] fn_get_docente_chat_context no disponible (userId={}): {}",
                    userId, e.getMessage());
            return "{}";
        }
    }
}
