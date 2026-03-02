package com.CLMTZ.Backend.repository.reinforcement.student.impl;

import com.CLMTZ.Backend.config.DynamicDataSourceService;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentCancelRequestResponseDTO;
import com.CLMTZ.Backend.repository.reinforcement.student.StudentRequestActionRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class StudentRequestActionRepositoryImpl implements StudentRequestActionRepository {

    private final DynamicDataSourceService dynamicDataSourceService;

    public StudentRequestActionRepositoryImpl(DynamicDataSourceService dynamicDataSourceService) {
        this.dynamicDataSourceService = dynamicDataSourceService;
    }

    private NamedParameterJdbcTemplate getJdbcTemplate() {
        return dynamicDataSourceService.getJdbcTemplate();
    }

    @Override
    public StudentCancelRequestResponseDTO cancelRequest(Integer userId, Integer requestId) {
        try {
            return cancelRequestUserFirst(userId, requestId);
        } catch (DataAccessException e) {
            String message = e.getMessage();
            if (message != null && (message.contains("function") || message.contains("does not exist") || message.contains("arguments"))) {
                return cancelRequestRequestFirst(userId, requestId);
            }
            throw e;
        }
    }

    private StudentCancelRequestResponseDTO cancelRequestUserFirst(Integer userId, Integer requestId) {
        String sql = "SELECT reforzamiento.fn_up_cancelar_solicitud_estudiante(:userId, :requestId) AS result";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("requestId", requestId);

        return executeAndInterpret(sql, params, requestId);
    }

    private StudentCancelRequestResponseDTO cancelRequestRequestFirst(Integer userId, Integer requestId) {
        String sql = "SELECT reforzamiento.fn_up_cancelar_solicitud_estudiante(:requestId, :userId) AS result";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("requestId", requestId);
        params.addValue("userId", userId);

        return executeAndInterpret(sql, params, requestId);
    }

    private StudentCancelRequestResponseDTO executeAndInterpret(String sql, MapSqlParameterSource params, Integer requestId) {
        Object result = getJdbcTemplate().queryForObject(sql, params, Object.class);

        if (result == null) {
            return new StudentCancelRequestResponseDTO(requestId, "CANCELLED", "Request cancelled successfully");
        }

        if (result instanceof Boolean) {
            Boolean boolResult = (Boolean) result;
            if (boolResult) {
                return new StudentCancelRequestResponseDTO(requestId, "CANCELLED", "Request cancelled successfully");
            } else {
                return new StudentCancelRequestResponseDTO(requestId, "NOT_MODIFIED", "Request could not be cancelled");
            }
        }

        if (result instanceof Number) {
            int intResult = ((Number) result).intValue();
            if (intResult == 1 || intResult > 0) {
                return new StudentCancelRequestResponseDTO(requestId, "CANCELLED", "Request cancelled successfully");
            } else {
                return new StudentCancelRequestResponseDTO(requestId, "NOT_MODIFIED", "Request could not be cancelled");
            }
        }

        return new StudentCancelRequestResponseDTO(requestId, "CANCELLED", "Request cancelled successfully");
    }
}