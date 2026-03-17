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
}
