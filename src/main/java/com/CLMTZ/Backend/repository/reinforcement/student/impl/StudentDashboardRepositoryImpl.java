package com.CLMTZ.Backend.repository.reinforcement.student.impl;

import com.CLMTZ.Backend.config.DynamicDataSourceService;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentDashboardDTO;
import com.CLMTZ.Backend.repository.reinforcement.student.StudentDashboardRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.List;

@Repository
public class StudentDashboardRepositoryImpl implements StudentDashboardRepository {

    private final DynamicDataSourceService dynamicDataSourceService;

    public StudentDashboardRepositoryImpl(DynamicDataSourceService dynamicDataSourceService) {
        this.dynamicDataSourceService = dynamicDataSourceService;
    }

    private NamedParameterJdbcTemplate getJdbcTemplate() {
        return dynamicDataSourceService.getJdbcTemplate();
    }

    @Override
    public StudentDashboardDTO getDashboard(Integer userId, Integer periodId) {
        try {
            return getDashboardWithTwoParams(userId, periodId);
        } catch (Exception e) {
            return getDashboardWithOneParam(userId);
        }
    }

    private StudentDashboardDTO getDashboardWithTwoParams(Integer userId, Integer periodId) {
        String sql = "SELECT * FROM reforzamiento.fn_sl_dashboard_estudiante_ui(:userId, :periodId)";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("periodId", periodId);

        return executeQuery(sql, params);
    }

    private StudentDashboardDTO getDashboardWithOneParam(Integer userId) {
        String sql = "SELECT * FROM reforzamiento.fn_sl_dashboard_estudiante_ui(:userId)";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);

        return executeQuery(sql, params);
    }

    private StudentDashboardDTO executeQuery(String sql, MapSqlParameterSource params) {
        List<StudentDashboardDTO> results = getJdbcTemplate().query(sql, params, (rs, rowNum) -> {
            StudentDashboardDTO dto = new StudentDashboardDTO();

            dto.setPending(getColumnValue(rs, "pendientes", "pending"));
            dto.setAccepted(getColumnValue(rs, "aceptadas", "accepted"));
            dto.setUpcoming(getColumnValue(rs, "proximas", "upcoming"));
            dto.setCompleted(getColumnValue(rs, "realizadas", "completed"));

            return dto;
        });

        if (results.isEmpty()) {
            return new StudentDashboardDTO(0L, 0L, 0L, 0L);
        }

        return results.get(0);
    }

    private Long getColumnValue(ResultSet rs, String spanishName, String englishName) {
        try {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i).toLowerCase();
                if (columnName.equals(spanishName.toLowerCase()) || columnName.equals(englishName.toLowerCase())) {
                    return rs.getLong(i);
                }
            }

            try {
                return rs.getLong(spanishName);
            } catch (Exception e1) {
                try {
                    return rs.getLong(englishName);
                } catch (Exception e2) {
                    return 0L;
                }
            }
        } catch (Exception e) {
            return 0L;
        }
    }
}