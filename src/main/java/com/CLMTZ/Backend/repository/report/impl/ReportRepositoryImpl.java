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
import java.util.LinkedHashMap;
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

    // ────────────────────────────────────────────────────────────────────────────
    //  PREVIEW ROWS — devuelven List<Map<String,Object>> con claves camelCase
    //  que coinciden exactamente con las column-keys del frontend.
    // ────────────────────────────────────────────────────────────────────────────

    private MapSqlParameterSource buildParams(Integer periodId, String dateFrom, String dateTo) {
        MapSqlParameterSource p = new MapSqlParameterSource("periodoId", periodId);
        if (dateFrom != null && !dateFrom.isBlank()) p.addValue("dateFrom", dateFrom);
        if (dateTo   != null && !dateTo.isBlank())   p.addValue("dateTo",   dateTo);
        return p;
    }

    private String dateFilter(String dateFrom, String dateTo, String alias) {
        StringBuilder sb = new StringBuilder();
        if (dateFrom != null && !dateFrom.isBlank())
            sb.append(" AND ").append(alias).append(".fechahoracreacion >= :dateFrom::timestamp");
        if (dateTo != null && !dateTo.isBlank())
            sb.append(" AND ").append(alias).append(".fechahoracreacion <= :dateTo::timestamp");
        return sb.toString();
    }

    @Override
    public List<Map<String, Object>> getPreviewBySubject(Integer periodId, String dateFrom, String dateTo) {
        String sql =
            "SELECT a.asignatura                                    AS asignatura, " +
            "       COUNT(sr.idsolicitudrefuerzo)                   AS \"totalMateria\", " +
            "       COUNT(CASE WHEN est.nombreestado ILIKE '%pendiente%' THEN 1 END) AS pendientes, " +
            "       COUNT(CASE WHEN est.nombreestado NOT ILIKE '%pendiente%' THEN 1 END) AS gestionadas " +
            "FROM reforzamiento.tbsolicitudesrefuerzos sr " +
            "JOIN academico.tbasignaturas a ON sr.idasignatura = a.idasignatura " +
            "JOIN reforzamiento.tbestadossolicitudesrefuerzos est ON sr.idestadosolicitudrefuerzo = est.idestadosolicitudrefuerzo " +
            "WHERE sr.idperiodo = :periodoId" +
            dateFilter(dateFrom, dateTo, "sr") +
            " GROUP BY a.idasignatura, a.asignatura " +
            "ORDER BY \"totalMateria\" DESC";
        try {
            return remap(getJdbcTemplate().queryForList(sql, buildParams(periodId, dateFrom, dateTo)));
        } catch (Exception e) { return Collections.emptyList(); }
    }

    @Override
    public List<Map<String, Object>> getPreviewByTeacher(Integer periodId, String dateFrom, String dateTo) {
        String sql =
            "SELECT CONCAT(u.nombres, ' ', u.apellidos)                             AS docente, " +
            "       a.asignatura                                                      AS materia, " +
            "       COUNT(DISTINCT rr.idrefuerzorealizado)                           AS sesiones, " +
            "       COUNT(DISTINCT sr.idestudiante)                                  AS estudiantes " +
            "FROM reforzamiento.tbsolicitudesrefuerzos sr " +
            "JOIN academico.tbdocentes doc ON sr.iddocente = doc.iddocente " +
            "JOIN general.tbusuarios u ON doc.idusuario = u.idusuario " +
            "JOIN academico.tbasignaturas a ON sr.idasignatura = a.idasignatura " +
            "LEFT JOIN reforzamiento.tbdetallesrefuerzosprogramadas d " +
            "       ON d.idsolicitudrefuerzo = sr.idsolicitudrefuerzo " +
            "LEFT JOIN reforzamiento.tbrefuerzosprogramados rp " +
            "       ON rp.idrefuerzoprogramado = d.idrefuerzoprogramado " +
            "LEFT JOIN reforzamiento.tbrefuerzosrealizados rr " +
            "       ON rr.idrefuerzoprogramado = rp.idrefuerzoprogramado " +
            "WHERE sr.idperiodo = :periodoId" +
            dateFilter(dateFrom, dateTo, "sr") +
            " GROUP BY doc.iddocente, u.nombres, u.apellidos, a.idasignatura, a.asignatura " +
            "ORDER BY sesiones DESC NULLS LAST";
        try {
            return remap(getJdbcTemplate().queryForList(sql, buildParams(periodId, dateFrom, dateTo)));
        } catch (Exception e) { return Collections.emptyList(); }
    }

    @Override
    public List<Map<String, Object>> getPreviewByParallel(Integer periodId, String dateFrom, String dateTo) {
        String sql =
            "SELECT p.paralelo                                                        AS paralelo, " +
            "       a.asignatura                                                      AS materia, " +
            "       COUNT(DISTINCT sr.idsolicitudrefuerzo)                           AS solicitudes, " +
            "       COALESCE(ROUND(100.0 * COUNT(CASE WHEN ar.asistencia = true  THEN 1 END) " +
            "           / NULLIF(COUNT(ar.idasistencia), 0), 1), 0)                  AS asistencia, " +
            "       COALESCE(ROUND(100.0 * COUNT(CASE WHEN ar.asistencia = false THEN 1 END) " +
            "           / NULLIF(COUNT(ar.idasistencia), 0), 1), 0)                  AS inasistencia " +
            "FROM reforzamiento.tbsolicitudesrefuerzos sr " +
            "JOIN academico.tbestudiantes est ON sr.idestudiante = est.idestudiante " +
            "JOIN academico.tbmatriculas m " +
            "       ON m.idestudiante = est.idestudiante AND m.idperiodo = sr.idperiodo " +
            "JOIN academico.tbdetallematricula dm ON dm.idmatricula = m.idmatricula " +
            "JOIN academico.tbparalelos p ON p.idparalelo = dm.idparalelo " +
            "JOIN academico.tbasignaturas a ON sr.idasignatura = a.idasignatura " +
            "LEFT JOIN reforzamiento.tbparticipantes part " +
            "       ON part.idsolicitudrefuerzo = sr.idsolicitudrefuerzo " +
            "LEFT JOIN reforzamiento.tbdetallesrefuerzosprogramadas d " +
            "       ON d.idsolicitudrefuerzo = sr.idsolicitudrefuerzo " +
            "LEFT JOIN reforzamiento.tbrefuerzosprogramados rp " +
            "       ON rp.idrefuerzoprogramado = d.idrefuerzoprogramado " +
            "LEFT JOIN reforzamiento.tbasistenciasrefuerzos ar " +
            "       ON ar.idrefuerzoprogramado = rp.idrefuerzoprogramado " +
            "      AND ar.idparticipante = part.idparticipante " +
            "WHERE sr.idperiodo = :periodoId" +
            dateFilter(dateFrom, dateTo, "sr") +
            " GROUP BY p.idparalelo, p.paralelo, a.idasignatura, a.asignatura " +
            "ORDER BY p.paralelo, a.asignatura";
        try {
            return remap(getJdbcTemplate().queryForList(sql, buildParams(periodId, dateFrom, dateTo)));
        } catch (Exception e) { return Collections.emptyList(); }
    }

    @Override
    public List<Map<String, Object>> getPreviewByGrade(Integer periodId, String dateFrom, String dateTo) {
        String sql =
            "SELECT CASE a.semestre " +
            "         WHEN 1 THEN '1er Semestre' WHEN 2 THEN '2do Semestre' " +
            "         WHEN 3 THEN '3er Semestre' WHEN 4 THEN '4to Semestre' " +
            "         WHEN 5 THEN '5to Semestre' WHEN 6 THEN '6to Semestre' " +
            "         WHEN 7 THEN '7mo Semestre' WHEN 8 THEN '8vo Semestre' " +
            "         WHEN 9 THEN '9no Semestre' WHEN 10 THEN '10mo Semestre' " +
            "         ELSE CONCAT(a.semestre::text, 'to Semestre') " +
            "       END                                                               AS curso, " +
            "       COUNT(DISTINCT sr.idsolicitudrefuerzo)                           AS solicitudes, " +
            "       COALESCE(ROUND(100.0 * COUNT(CASE WHEN ar.asistencia = true  THEN 1 END) " +
            "           / NULLIF(COUNT(ar.idasistencia), 0), 1), 0)                  AS asistencia, " +
            "       COALESCE(ROUND(100.0 * COUNT(CASE WHEN ar.asistencia = false THEN 1 END) " +
            "           / NULLIF(COUNT(ar.idasistencia), 0), 1), 0)                  AS inasistencia " +
            "FROM reforzamiento.tbsolicitudesrefuerzos sr " +
            "JOIN academico.tbasignaturas a ON sr.idasignatura = a.idasignatura " +
            "LEFT JOIN reforzamiento.tbparticipantes part " +
            "       ON part.idsolicitudrefuerzo = sr.idsolicitudrefuerzo " +
            "LEFT JOIN reforzamiento.tbdetallesrefuerzosprogramadas d " +
            "       ON d.idsolicitudrefuerzo = sr.idsolicitudrefuerzo " +
            "LEFT JOIN reforzamiento.tbrefuerzosprogramados rp " +
            "       ON rp.idrefuerzoprogramado = d.idrefuerzoprogramado " +
            "LEFT JOIN reforzamiento.tbasistenciasrefuerzos ar " +
            "       ON ar.idrefuerzoprogramado = rp.idrefuerzoprogramado " +
            "      AND ar.idparticipante = part.idparticipante " +
            "WHERE sr.idperiodo = :periodoId" +
            dateFilter(dateFrom, dateTo, "sr") +
            " GROUP BY a.semestre " +
            "ORDER BY a.semestre";
        try {
            return remap(getJdbcTemplate().queryForList(sql, buildParams(periodId, dateFrom, dateTo)));
        } catch (Exception e) { return Collections.emptyList(); }
    }

    @Override
    public List<Map<String, Object>> getPreviewByStudent(Integer periodId, String dateFrom, String dateTo) {
        String sql =
            "SELECT CONCAT(u.nombres, ' ', u.apellidos)                             AS estudiante, " +
            "       COUNT(DISTINCT sr.idsolicitudrefuerzo)                           AS solicitudes, " +
            "       (SELECT a2.asignatura " +
            "        FROM reforzamiento.tbsolicitudesrefuerzos sr2 " +
            "        JOIN academico.tbasignaturas a2 ON sr2.idasignatura = a2.idasignatura " +
            "        WHERE sr2.idestudiante = sr.idestudiante " +
            "          AND sr2.idperiodo = :periodoId " +
            "        GROUP BY a2.idasignatura, a2.asignatura " +
            "        ORDER BY COUNT(*) DESC LIMIT 1)                                 AS \"materia_refuerzo\", " +
            "       COALESCE(CONCAT(ROUND(100.0 * COUNT(CASE WHEN ar.asistencia = true THEN 1 END) " +
            "           / NULLIF(COUNT(ar.idasistencia), 0), 1)::text, '%'), '0%')  AS asistencia " +
            "FROM reforzamiento.tbsolicitudesrefuerzos sr " +
            "JOIN academico.tbestudiantes ests ON sr.idestudiante = ests.idestudiante " +
            "JOIN general.tbusuarios u ON ests.idusuario = u.idusuario " +
            "LEFT JOIN reforzamiento.tbparticipantes part " +
            "       ON part.idsolicitudrefuerzo = sr.idsolicitudrefuerzo " +
            "LEFT JOIN reforzamiento.tbdetallesrefuerzosprogramadas d " +
            "       ON d.idsolicitudrefuerzo = sr.idsolicitudrefuerzo " +
            "LEFT JOIN reforzamiento.tbrefuerzosprogramados rp " +
            "       ON rp.idrefuerzoprogramado = d.idrefuerzoprogramado " +
            "LEFT JOIN reforzamiento.tbasistenciasrefuerzos ar " +
            "       ON ar.idrefuerzoprogramado = rp.idrefuerzoprogramado " +
            "      AND ar.idparticipante = part.idparticipante " +
            "WHERE sr.idperiodo = :periodoId" +
            dateFilter(dateFrom, dateTo, "sr") +
            " GROUP BY ests.idestudiante, u.nombres, u.apellidos " +
            "ORDER BY solicitudes DESC";
        try {
            return remap(getJdbcTemplate().queryForList(sql, buildParams(periodId, dateFrom, dateTo)));
        } catch (Exception e) { return Collections.emptyList(); }
    }

    /**
     * JDBC devuelve las claves en minúsculas (PostgreSQL pliega a minúsculas los
     * identificadores sin comillas). Necesitamos devolver un mapa con la misma
     * capitalización que espera el frontend, por eso se re-mapea manualmente.
     */
    private List<Map<String, Object>> remap(List<Map<String, Object>> rows) {
        // totalMateria puede llegar como "totalMateria" (si lo citamos) o "totalmateria"
        // Normalizamos aquí cualquier clave conocida a su forma camelCase.
        rows.forEach(row -> {
            renameKey(row, "totalmateria",    "totalMateria");
            renameKey(row, "materia_refuerzo","materia_refuerzo"); // ya está bien
        });
        return rows;
    }

    private void renameKey(Map<String, Object> map, String oldKey, String newKey) {
        if (!oldKey.equals(newKey) && map.containsKey(oldKey)) {
            map.put(newKey, map.remove(oldKey));
        }
    }
}
