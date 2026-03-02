package com.CLMTZ.Backend.repository.reinforcement.student.impl;

import com.CLMTZ.Backend.config.DynamicDataSourceService;
import com.CLMTZ.Backend.repository.reinforcement.student.StudentRequestRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class StudentRequestRepositoryImpl implements StudentRequestRepository {

    private final DynamicDataSourceService dynamicDataSourceService;

    public StudentRequestRepositoryImpl(DynamicDataSourceService dynamicDataSourceService) {
        this.dynamicDataSourceService = dynamicDataSourceService;
    }

    private NamedParameterJdbcTemplate getJdbcTemplate() {
        return dynamicDataSourceService.getJdbcTemplate();
    }

    /**
     * Crea solicitud llamando a la función fn_in_nueva_solicitud_estudiante_v2.
     */
    @Override
    public Integer createRequest(Integer userId, Integer subjectId, Integer teacherId,
                                 Integer sessionTypeId, String reason, Integer periodId) {
        String sql = "SELECT reforzamiento.fn_in_nueva_solicitud_estudiante_v2(" +
                ":userId, :subjectId, :teacherId, :sessionTypeId, :reason, :periodId)";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("subjectId", subjectId);
        params.addValue("teacherId", teacherId);
        params.addValue("sessionTypeId", sessionTypeId);
        params.addValue("reason", reason);
        params.addValue("periodId", periodId);

        Integer requestId = getJdbcTemplate().queryForObject(sql, params, Integer.class);

        if (requestId == null) {
            throw new IllegalStateException("Failed to create request: no ID returned");
        }

        return requestId;
    }

    @Override
    public void addParticipants(Integer requestId, List<Integer> studentIds) {
        String sql = "SELECT reforzamiento.fn_in_participante_solicitud(:requestId, :studentId)";

        for (Integer studentId : studentIds) {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("requestId", requestId);
            params.addValue("studentId", studentId);
            getJdbcTemplate().queryForObject(sql, params, Integer.class);
        }
    }
}