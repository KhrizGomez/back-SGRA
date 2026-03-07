package com.CLMTZ.Backend.repository.report.impl;

import com.CLMTZ.Backend.config.DynamicDataSourceService;
import com.CLMTZ.Backend.dto.reinforcement.coordination.*;
import com.CLMTZ.Backend.dto.report.AttendanceDetailRowDTO;
import com.CLMTZ.Backend.dto.report.RequestDetailRowDTO;
import com.CLMTZ.Backend.repository.report.ReportRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Repository
public class ReportRepositoryImpl implements ReportRepository {

    private final DynamicDataSourceService dynamicDataSourceService;

    public ReportRepositoryImpl(DynamicDataSourceService dynamicDataSourceService) {
        this.dynamicDataSourceService = dynamicDataSourceService;
    }

    private NamedParameterJdbcTemplate getJdbcTemplate() {
        return dynamicDataSourceService.getJdbcTemplate();
    }

    @Override
    public Integer getActivePeriodId() {
        String sql = "SELECT academico.fn_sl_id_periodo_activo()";
        return getJdbcTemplate().getJdbcTemplate().queryForObject(sql, Integer.class);
    }

    @Override
    public String getActivePeriodName() {
        String sql = "SELECT nombreperiodo FROM academico.tbperiodos WHERE idperiodo = :periodId";
        MapSqlParameterSource params = new MapSqlParameterSource("periodId", getActivePeriodId());
        try {
            return getJdbcTemplate().queryForObject(sql, params, String.class);
        } catch (Exception e) {
            return "Periodo Actual";
        }
    }

    @Override
    public CoordinationDashboardKpisDTO getKpis(Integer periodId) {
        String sql = "SELECT total_solicitudes, pendientes, gestionadas " +
                     "FROM reforzamiento.vw_dashboard_kpis_solicitudes " +
                     "WHERE idperiodo = :periodoId";
        MapSqlParameterSource params = new MapSqlParameterSource("periodoId", periodId);

        List<CoordinationDashboardKpisDTO> results = getJdbcTemplate().query(sql, params, (rs, rowNum) ->
                new CoordinationDashboardKpisDTO(
                        rs.getLong("total_solicitudes"),
                        rs.getLong("pendientes"),
                        rs.getLong("gestionadas")
                ));
        return results.isEmpty() ? new CoordinationDashboardKpisDTO(0L, 0L, 0L) : results.get(0);
    }

    @Override
    public CoordinationDashboardAsistenciaDTO getAsistencia(Integer periodId) {
        String sql = "SELECT total_sesiones_registradas, total_asistencias, total_inasistencias, " +
                     "porcentaje_asistencia, tasa_inasistencia " +
                     "FROM reforzamiento.vw_dashboard_asistencia " +
                     "WHERE idperiodo = :periodoId";
        MapSqlParameterSource params = new MapSqlParameterSource("periodoId", periodId);

        List<CoordinationDashboardAsistenciaDTO> results = getJdbcTemplate().query(sql, params, (rs, rowNum) ->
                new CoordinationDashboardAsistenciaDTO(
                        rs.getLong("total_sesiones_registradas"),
                        rs.getLong("total_asistencias"),
                        rs.getLong("total_inasistencias"),
                        rs.getBigDecimal("porcentaje_asistencia"),
                        rs.getBigDecimal("tasa_inasistencia")
                ));
        return results.isEmpty()
                ? new CoordinationDashboardAsistenciaDTO(0L, 0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO)
                : results.get(0);
    }

    @Override
    public List<CoordinationDashboardSolicitudesMateriaDTO> getSolicitudesPorMateria(Integer periodId) {
        String sql = "SELECT asignatura, total_materia, pendientes, gestionadas " +
                     "FROM reforzamiento.vw_dashboard_solicitudes_materia " +
                     "WHERE idperiodo = :periodoId";
        MapSqlParameterSource params = new MapSqlParameterSource("periodoId", periodId);

        return getJdbcTemplate().query(sql, params, (rs, rowNum) ->
                new CoordinationDashboardSolicitudesMateriaDTO(
                        rs.getString("asignatura"),
                        rs.getLong("total_materia"),
                        rs.getLong("pendientes"),
                        rs.getLong("gestionadas")
                ));
    }

    @Override
    public List<CoordinationDashboardModalidadDTO> getModalidades(Integer periodId) {
        String sql = "SELECT modalidad, total " +
                     "FROM reforzamiento.vw_dashboard_modalidad " +
                     "WHERE idperiodo = :periodoId";
        MapSqlParameterSource params = new MapSqlParameterSource("periodoId", periodId);

        return getJdbcTemplate().query(sql, params, (rs, rowNum) ->
                new CoordinationDashboardModalidadDTO(
                        rs.getString("modalidad"),
                        rs.getLong("total")
                ));
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ────────────────────────────────────────────────────────────────────────────


    private MapSqlParameterSource buildFunctionDateParams(Integer periodId, String dateFrom, String dateTo) {
        return new MapSqlParameterSource()
                .addValue("periodoId", periodId)
                .addValue("dateFrom", (dateFrom == null || dateFrom.isBlank()) ? null : dateFrom)
                .addValue("dateTo", (dateTo == null || dateTo.isBlank()) ? null : dateTo);
    }


    // ────────────────────────────────────────────────────────────────────────────
    //  DETAIL ROWS — llaman a funciones de BD
    // ────────────────────────────────────────────────────────────────────────────

    @Override
    public List<AttendanceDetailRowDTO> getAttendanceDetails(Integer periodId) {
        String sql = "SELECT * FROM reforzamiento.fn_sl_reporte_detalles_asistencia(:periodoId)";
        MapSqlParameterSource params = new MapSqlParameterSource("periodoId", periodId);
        try {
            return getJdbcTemplate().query(sql, params, (rs, rowNum) ->
                    new AttendanceDetailRowDTO(
                            rs.getInt("session_id"),
                            rs.getString("student_name"),
                            rs.getString("subject_name"),
                            rs.getString("teacher_name"),
                            rs.getString("session_date"),
                            rs.getString("session_type"),
                            rs.getObject("attended") != null ? rs.getBoolean("attended") : null,
                            rs.getString("duration"),
                            rs.getString("notes")
                    ));
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public List<RequestDetailRowDTO> getRequestDetails(Integer periodId) {
        String sql = "SELECT * FROM reforzamiento.fn_sl_reporte_detalles_solicitudes(:periodoId)";
        MapSqlParameterSource params = new MapSqlParameterSource("periodoId", periodId);
        try {
            return getJdbcTemplate().query(sql, params, (rs, rowNum) ->
                    new RequestDetailRowDTO(
                            rs.getInt("request_id"),
                            rs.getString("created_at"),
                            rs.getString("student_name"),
                            rs.getString("subject_name"),
                            rs.getString("teacher_name"),
                            rs.getString("session_type"),
                            rs.getString("status_name"),
                            rs.getString("reason")
                    ));
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }


    // ────────────────────────────────────────────────────────────────────────────
    //  PREVIEW ROWS — llaman a funciones de BD
    // ────────────────────────────────────────────────────────────────────────────

    @Override
    public List<Map<String, Object>> getPreviewBySubject(Integer periodId, String dateFrom, String dateTo) {
        String sql = "SELECT * FROM reforzamiento.fn_sl_reporte_preview_materia(" +
                     ":periodoId, :dateFrom::timestamp, :dateTo::timestamp)";
        try {
            return getJdbcTemplate().queryForList(sql, buildFunctionDateParams(periodId, dateFrom, dateTo));
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getPreviewByTeacher(Integer periodId, String dateFrom, String dateTo) {
        String sql = "SELECT * FROM reforzamiento.fn_sl_reporte_preview_docente(" +
                     ":periodoId, :dateFrom::timestamp, :dateTo::timestamp)";
        try {
            return getJdbcTemplate().queryForList(sql, buildFunctionDateParams(periodId, dateFrom, dateTo));
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getPreviewByParallel(Integer periodId, String dateFrom, String dateTo) {
        String sql = "SELECT * FROM reforzamiento.fn_sl_reporte_preview_paralelo(" +
                     ":periodoId, :dateFrom::timestamp, :dateTo::timestamp)";
        try {
            return getJdbcTemplate().queryForList(sql, buildFunctionDateParams(periodId, dateFrom, dateTo));
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getPreviewByGrade(Integer periodId, String dateFrom, String dateTo) {
        String sql = "SELECT * FROM reforzamiento.fn_sl_reporte_preview_curso(" +
                     ":periodoId, :dateFrom::timestamp, :dateTo::timestamp)";
        try {
            return getJdbcTemplate().queryForList(sql, buildFunctionDateParams(periodId, dateFrom, dateTo));
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getPreviewByStudent(Integer periodId, String dateFrom, String dateTo) {
        String sql = "SELECT * FROM reforzamiento.fn_sl_reporte_preview_estudiante(" +
                     ":periodoId, :dateFrom::timestamp, :dateTo::timestamp)";
        try {
            return getJdbcTemplate().queryForList(sql, buildFunctionDateParams(periodId, dateFrom, dateTo));
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
