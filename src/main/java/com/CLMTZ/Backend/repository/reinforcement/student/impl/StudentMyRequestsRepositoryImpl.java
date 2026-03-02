package com.CLMTZ.Backend.repository.reinforcement.student.impl;

import com.CLMTZ.Backend.config.DynamicDataSourceService;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentMyRequestItemDTO;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentMyRequestsChipsDTO;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentMyRequestsPageDTO;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentMyRequestsStatusSummaryDTO;
import com.CLMTZ.Backend.repository.reinforcement.student.StudentMyRequestsRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Repository
public class StudentMyRequestsRepositoryImpl implements StudentMyRequestsRepository {

    private final DynamicDataSourceService dynamicDataSourceService;

    public StudentMyRequestsRepositoryImpl(DynamicDataSourceService dynamicDataSourceService) {
        this.dynamicDataSourceService = dynamicDataSourceService;
    }

    private NamedParameterJdbcTemplate getJdbcTemplate() {
        return dynamicDataSourceService.getJdbcTemplate();
    }

    @Override
    public StudentMyRequestsPageDTO getMyRequests(Integer userId, Integer periodId, Integer statusId,
                                                   Integer sessionTypeId, Integer subjectId, String search,
                                                   Integer page, Integer size) {
        String sql = "SELECT * FROM reforzamiento.fn_sl_mis_solicitudes_ui(" +
                ":userId, :periodId, :statusId, :sessionTypeId, :subjectId, :search, :page, :size)";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("periodId", periodId);
        params.addValue("statusId", statusId);
        params.addValue("sessionTypeId", sessionTypeId);
        params.addValue("subjectId", subjectId);
        params.addValue("search", search);
        params.addValue("page", page);
        params.addValue("size", size);

        List<StudentMyRequestItemDTO> items = new ArrayList<>();
        final Long[] totalCount = {0L};

        getJdbcTemplate().query(sql, params, (rs) -> {
            StudentMyRequestItemDTO item = new StudentMyRequestItemDTO();
            item.setRequestId(rs.getInt("idsolicitudrefuerzo"));

            Timestamp timestamp = rs.getTimestamp("fecha_hora");
            item.setRequestDateTime(timestamp != null ? timestamp.toInstant().toString() : null);

            item.setSubjectCode(rs.getString("asignatura_codigo"));
            item.setSubjectName(rs.getString("asignatura_nombre"));
            item.setTopic(rs.getString("tema"));
            item.setTeacherName(rs.getString("docente"));
            item.setSessionType(rs.getString("tipo"));
            item.setStatus(rs.getString("estado"));

            if (items.isEmpty()) {
                totalCount[0] = rs.getLong("total_count");
            }

            items.add(item);
        });

        return new StudentMyRequestsPageDTO(items, totalCount[0], page, size);
    }

    @Override
    public StudentMyRequestsChipsDTO getMyRequestsChips(Integer userId, Integer periodId) {
        String sql = "SELECT * FROM reforzamiento.fn_sl_mis_solicitudes_chips(:userId, :periodId)";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("periodId", periodId);

        List<StudentMyRequestsChipsDTO> results = getJdbcTemplate().query(sql, params, (rs, rowNum) -> {
            StudentMyRequestsChipsDTO dto = new StudentMyRequestsChipsDTO();
            dto.setPending(rs.getLong("pendientes"));
            dto.setAccepted(rs.getLong("aceptadas"));
            dto.setScheduled(rs.getLong("programadas"));
            dto.setCompleted(rs.getLong("completadas"));
            return dto;
        });

        if (results.isEmpty()) {
            return new StudentMyRequestsChipsDTO(0L, 0L, 0L, 0L);
        }

        return results.get(0);
    }

    @Override
    public List<StudentMyRequestsStatusSummaryDTO> getMyRequestsSummary(Integer userId, Integer periodId) {
        String sql = "SELECT * FROM reforzamiento.fn_sl_mis_solicitudes_resumen(:userId, :periodId)";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("periodId", periodId);

        return getJdbcTemplate().query(sql, params, (rs, rowNum) -> {
            StudentMyRequestsStatusSummaryDTO dto = new StudentMyRequestsStatusSummaryDTO();
            dto.setStatusId(rs.getInt("estado_id"));
            dto.setStatusJson(rs.getString("estado"));
            dto.setTotal(rs.getLong("total"));
            return dto;
        });
    }
}