package com.CLMTZ.Backend.repository.ai;

import com.CLMTZ.Backend.config.DynamicDataSourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ExcelValidationContextRepository {

    private final DynamicDataSourceService dynamicDataSourceService;

    public String getTeachersContext() {
        return callFunction("SELECT academico.fn_get_validation_context_teachers()");
    }

    public String getRegistrationsContext() {
        return callFunction("SELECT academico.fn_get_validation_context_registrations()");
    }

    public String getClassSchedulesContext() {
        return callFunction("SELECT academico.fn_get_validation_context_class_schedules()");
    }

    private String callFunction(String sql) {
        try {
            return dynamicDataSourceService.getJdbcTemplate()
                    .getJdbcTemplate()
                    .queryForObject(sql, String.class);
        } catch (Exception e) {
            log.warn("[ExcelValidationContextRepository] Error al llamar función '{}': {}", sql, e.getMessage());
            return null;
        }
    }
}
