package com.CLMTZ.Backend.repository.reinforcement.student.impl;

import com.CLMTZ.Backend.config.DynamicDataSourceService;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentRequestSummaryDTO;
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
                                 Integer sessionTypeId, String reason, Integer periodId,
                                 Short preferredDayOfWeek, Integer preferredTimeSlotId) {
        String sql = "SELECT reforzamiento.fn_in_nueva_solicitud_estudiante_v2(" +
                ":userId, :subjectId, :teacherId, :sessionTypeId, :reason, :periodId, " +
                ":preferredDayOfWeek, :preferredTimeSlotId)";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("subjectId", subjectId);
        params.addValue("teacherId", teacherId);
        params.addValue("sessionTypeId", sessionTypeId);
        params.addValue("reason", reason);
        params.addValue("periodId", periodId);
        params.addValue("preferredDayOfWeek", preferredDayOfWeek);
        params.addValue("preferredTimeSlotId", preferredTimeSlotId);

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

    @Override
    public StudentRequestSummaryDTO getRequestSummary(Integer requestId) {
        String sql = "SELECT * FROM reforzamiento.fn_sl_resumen_solicitud_notif(:requestId)";

        MapSqlParameterSource params = new MapSqlParameterSource("requestId", requestId);

        return getJdbcTemplate().query(sql, params, rs -> rs.next()
                ? new StudentRequestSummaryDTO(
                        rs.getInt("request_id"),
                        rs.getString("student_name"),
                        rs.getString("student_email"),
                        rs.getString("teacher_name"),
                        rs.getString("teacher_email"),
                        rs.getString("subject_name"),
                        rs.getString("course_name"),
                        rs.getString("parallel_name"),
                        rs.getString("reason"))
                : null);
    }
}