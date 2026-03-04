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

    @Override
    public List<AttendanceDetailRowDTO> getAttendanceDetails(Integer periodId) {
        String sql = "SELECT rr.idrefuerzorealizado AS session_id, " +
                     "CONCAT(u.nombres, ' ', u.apellidos) AS student_name, " +
                     "a.asignatura AS subject_name, " +
                     "CONCAT(ud.nombres, ' ', ud.apellidos) AS teacher_name, " +
                     "rp.fechaprogramadarefuerzo::text AS session_date, " +
                     "ts.tiposesion AS session_type, " +
                     "ar.asistencia AS attended, " +
                     "rr.duracion::text AS duration, " +
                     "rr.observacion AS notes " +
                     "FROM reforzamiento.tbrefuerzosrealizados rr " +
                     "JOIN reforzamiento.tbrefuerzosprogramados rp ON rr.idrefuerzoprogramado = rp.idrefuerzoprogramado " +
                     "JOIN reforzamiento.tbtipossesiones ts ON rp.idtiposesion = ts.idtiposesion " +
                     "JOIN reforzamiento.tbdetallesrefuerzosprogramadas d ON rp.idrefuerzoprogramado = d.idrefuerzoprogramado " +
                     "JOIN reforzamiento.tbsolicitudesrefuerzos sr ON d.idsolicitudrefuerzo = sr.idsolicitudrefuerzo " +
                     "JOIN academico.tbasignaturas a ON sr.idasignatura = a.idasignatura " +
                     "JOIN academico.tbestudiantes e ON sr.idestudiante = e.idestudiante " +
                     "JOIN general.tbusuarios u ON e.idusuario = u.idusuario " +
                     "JOIN academico.tbdocentes doc ON sr.iddocente = doc.iddocente " +
                     "JOIN general.tbusuarios ud ON doc.idusuario = ud.idusuario " +
                     "LEFT JOIN reforzamiento.tbasistenciasrefuerzos ar ON ar.idrefuerzorealizado = rr.idrefuerzorealizado " +
                     "WHERE sr.idperiodo = :periodoId " +
                     "ORDER BY rp.fechaprogramadarefuerzo DESC, u.apellidos ASC";
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
        String sql = "SELECT sr.idsolicitudrefuerzo AS request_id, " +
                     "sr.fechahoracreacion::text AS created_at, " +
                     "CONCAT(u.nombres, ' ', u.apellidos) AS student_name, " +
                     "a.asignatura AS subject_name, " +
                     "CONCAT(ud.nombres, ' ', ud.apellidos) AS teacher_name, " +
                     "ts.tiposesion AS session_type, " +
                     "est.nombreestado AS status_name, " +
                     "sr.motivo AS reason " +
                     "FROM reforzamiento.tbsolicitudesrefuerzos sr " +
                     "JOIN academico.tbestudiantes e ON sr.idestudiante = e.idestudiante " +
                     "JOIN general.tbusuarios u ON e.idusuario = u.idusuario " +
                     "JOIN academico.tbasignaturas a ON sr.idasignatura = a.idasignatura " +
                     "JOIN academico.tbdocentes doc ON sr.iddocente = doc.iddocente " +
                     "JOIN general.tbusuarios ud ON doc.idusuario = ud.idusuario " +
                     "JOIN reforzamiento.tbtipossesiones ts ON sr.idtiposesion = ts.idtiposesion " +
                     "JOIN reforzamiento.tbestadossolicitudesrefuerzos est ON sr.idestadosolicitudrefuerzo = est.idestadosolicitudrefuerzo " +
                     "WHERE sr.idperiodo = :periodoId " +
                     "ORDER BY sr.fechahoracreacion DESC";
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
}
