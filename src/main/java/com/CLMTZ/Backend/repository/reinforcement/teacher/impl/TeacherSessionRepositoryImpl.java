package com.CLMTZ.Backend.repository.reinforcement.teacher.impl;

import com.CLMTZ.Backend.config.DynamicDataSourceService;
import com.CLMTZ.Backend.dto.reinforcement.teacher.AttendanceItemDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.ParticipantAttendanceDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherActionResponseDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherActiveSessionItemDTO;
import com.CLMTZ.Backend.repository.reinforcement.teacher.TeacherSessionRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

@Repository
public class TeacherSessionRepositoryImpl implements TeacherSessionRepository {

    private final DynamicDataSourceService dynamicDataSourceService;

    public TeacherSessionRepositoryImpl(DynamicDataSourceService dynamicDataSourceService) {
        this.dynamicDataSourceService = dynamicDataSourceService;
    }

    private NamedParameterJdbcTemplate getJdbcTemplate() {
        return dynamicDataSourceService.getJdbcTemplate();
    }

    private Integer getTeacherId(Integer userId) {
        String sql = "SELECT iddocente FROM academico.tbdocentes WHERE idusuario = :userId AND estado = TRUE";
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        return getJdbcTemplate().queryForObject(sql, params, Integer.class);
    }

    private boolean validateTeacherOwnsSession(Integer teacherId, Integer scheduledId) {
        String sql = "SELECT COUNT(*) " +
                "FROM reforzamiento.tbdetallesrefuerzosprogramadas d " +
                "JOIN reforzamiento.tbsolicitudesrefuerzos sr ON d.idsolicitudrefuerzo = sr.idsolicitudrefuerzo " +
                "WHERE d.idrefuerzoprogramado = :scheduledId AND sr.iddocente = :teacherId";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("scheduledId", scheduledId);
        params.addValue("teacherId", teacherId);
        Integer count = getJdbcTemplate().queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    private Integer getScheduledStatusId(String statusName) {
        String sql = "SELECT idestadorefuerzoprogramado FROM reforzamiento.tbestadosrefuerzosprogramados " +
                "WHERE LOWER(estadorefuerzoprogramado) = LOWER(:statusName) LIMIT 1";
        List<Integer> rows = getJdbcTemplate().queryForList(
                sql, new MapSqlParameterSource("statusName", statusName), Integer.class);
        if (rows.isEmpty()) {
            throw new RuntimeException("Estado de sesión '" + statusName + "' no encontrado en tbestadosrefuerzosprogramados");
        }
        return rows.get(0);
    }

    /**
     * Returns sessions with status 'Espera espacio', 'Reprogramado' or 'Programado' for the authenticated teacher.
     */
    @Override
    public List<TeacherActiveSessionItemDTO> getActiveSessions(Integer userId) {
        String sql = "SELECT DISTINCT ON (rp.idrefuerzoprogramado) " +
                "rp.idrefuerzoprogramado AS scheduled_id, " +
                "a.asignatura AS subject_name, " +
                "rp.fechaprogramadarefuerzo AS scheduled_date, " +
                "fh.horainicio AS start_time, " +
                "fh.horariofin AS end_time, " +
                "m.modalidad AS modality, " +
                "rp.duracionestimado AS estimated_duration, " +
                "est.estadorefuerzoprogramado AS status_name, " +
                "ts.tiposesion AS session_type, " +
                "(SELECT COUNT(*) FROM reforzamiento.tbdetallesrefuerzosprogramadas dd " +
                " WHERE dd.idrefuerzoprogramado = rp.idrefuerzoprogramado) AS participant_count, " +
                "(SELECT r.urlarchivorefuerzoprogramado FROM reforzamiento.tbrecursosrefuerzosprogramados r " +
                " WHERE r.idrefuerzoprogramado = rp.idrefuerzoprogramado " +
                " AND (r.urlarchivorefuerzoprogramado LIKE 'virtual_link:%' OR r.urlarchivorefuerzoprogramado LIKE 'link:%') LIMIT 1) AS virtual_link " +
                "FROM reforzamiento.tbrefuerzosprogramados rp " +
                "JOIN reforzamiento.tbestadosrefuerzosprogramados est " +
                "  ON rp.idestadorefuerzoprogramado = est.idestadorefuerzoprogramado " +
                "JOIN academico.tbmodalidades m ON rp.idmodalidad = m.idmodalidad " +
                "JOIN academico.tbfranjashorarias fh ON rp.idfranjahoraria = fh.idfranjahoraria " +
                "JOIN reforzamiento.tbtipossesiones ts ON rp.idtiposesion = ts.idtiposesion " +
                "JOIN reforzamiento.tbdetallesrefuerzosprogramadas d " +
                "  ON rp.idrefuerzoprogramado = d.idrefuerzoprogramado " +
                "JOIN reforzamiento.tbsolicitudesrefuerzos sr " +
                "  ON d.idsolicitudrefuerzo = sr.idsolicitudrefuerzo " +
                "JOIN academico.tbasignaturas a ON sr.idasignatura = a.idasignatura " +
                "JOIN academico.tbdocentes doc ON sr.iddocente = doc.iddocente " +
                "WHERE doc.idusuario = :userId " +
                "AND LOWER(est.estadorefuerzoprogramado) IN ('espera espacio', 'reprogramado', 'programado') " +
                "ORDER BY rp.idrefuerzoprogramado DESC, rp.fechaprogramadarefuerzo ASC";

        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        List<TeacherActiveSessionItemDTO> items = new ArrayList<>();

        getJdbcTemplate().query(sql, params, rs -> {
            TeacherActiveSessionItemDTO item = new TeacherActiveSessionItemDTO();
            item.setScheduledId(rs.getInt("scheduled_id"));
            item.setSubjectName(rs.getString("subject_name"));
            Date date = rs.getDate("scheduled_date");
            item.setScheduledDate(date != null ? date.toString() : null);
            Time st = rs.getTime("start_time");
            item.setStartTime(st != null ? st.toString().substring(0, 5) : null);
            Time et = rs.getTime("end_time");
            item.setEndTime(et != null ? et.toString().substring(0, 5) : null);
            item.setModality(rs.getString("modality"));
            Time dur = rs.getTime("estimated_duration");
            item.setEstimatedDuration(dur != null ? dur.toString().substring(0, 5) : null);
            item.setStatusName(rs.getString("status_name"));
            item.setSessionType(rs.getString("session_type"));
            item.setParticipantCount(rs.getInt("participant_count"));
            String vl = rs.getString("virtual_link");
            // Strip the "virtual_link:" or "link:" prefix if present
            if (vl != null) {
                if (vl.startsWith("virtual_link:")) vl = vl.replace("virtual_link:", "");
                else if (vl.startsWith("link:")) vl = vl.replace("link:", "");
                item.setVirtualLink(vl.trim());
            } else {
                item.setVirtualLink(null);
            }
            items.add(item);
        });

        return items;
    }

    /**
     * GET attendance list for a scheduled session (uses idrefuerzoprogramado).
     */
    @Override
    public List<ParticipantAttendanceDTO> getSessionAttendance(Integer userId, Integer scheduledId) {
        String sql = "SELECT a.idasistencia, a.idparticipante, " +
                "CONCAT(u.nombres, ' ', u.apellidos) AS student_name, a.asistencia " +
                "FROM reforzamiento.tbasistenciasrefuerzos a " +
                "JOIN reforzamiento.tbparticipantes p ON a.idparticipante = p.idparticipante " +
                "JOIN academico.tbestudiantes e ON p.idestudiante = e.idestudiante " +
                "JOIN general.tbusuarios u ON e.idusuario = u.idusuario " +
                "WHERE a.idrefuerzoprogramado = :scheduledId " +
                "ORDER BY u.apellidos, u.nombres";

        List<ParticipantAttendanceDTO> result = new ArrayList<>();
        getJdbcTemplate().query(sql, new MapSqlParameterSource("scheduledId", scheduledId), rs -> {
            result.add(new ParticipantAttendanceDTO(
                    rs.getInt("idasistencia"),
                    rs.getInt("idparticipante"),
                    rs.getString("student_name"),
                    rs.getBoolean("asistencia")));
        });
        return result;
    }

    @Override
    public List<String> getSessionResources(Integer userId, Integer scheduledId) {
        Integer teacherId = getTeacherId(userId);
        if (!validateTeacherOwnsSession(teacherId, scheduledId)) {
            throw new IllegalArgumentException("Sesión no encontrada o no pertenece a este docente");
        }

        String sql = "SELECT urlarchivorefuerzoprogramado " +
                "FROM reforzamiento.tbrecursosrefuerzosprogramados " +
                "WHERE idrefuerzoprogramado = :scheduledId " +
                "AND urlarchivorefuerzoprogramado NOT LIKE 'virtual_link:%' " +
                "AND urlarchivorefuerzoprogramado NOT LIKE 'link:%' " +
                "ORDER BY idrecursorefuerzoprogramado DESC";

        return getJdbcTemplate().queryForList(
                sql,
                new MapSqlParameterSource("scheduledId", scheduledId),
                String.class
        );
    }

    @Override
    public List<String> getSessionRequestResources(Integer userId, Integer scheduledId) {
        Integer teacherId = getTeacherId(userId);
        if (!validateTeacherOwnsSession(teacherId, scheduledId)) {
            throw new IllegalArgumentException("Sesión no encontrada o no pertenece a este docente");
        }

        String sql = "SELECT DISTINCT rr.urlarchivosolicitudrefuerzo " +
                "FROM reforzamiento.tbdetallesrefuerzosprogramadas d " +
                "JOIN reforzamiento.tbrecursossolicitudesrefuerzos rr " +
                "  ON rr.idsolicitudrefuerzo = d.idsolicitudrefuerzo " +
                "WHERE d.idrefuerzoprogramado = :scheduledId " +
                "ORDER BY rr.urlarchivosolicitudrefuerzo";

        return getJdbcTemplate().queryForList(
                sql,
                new MapSqlParameterSource("scheduledId", scheduledId),
                String.class
        );
    }

    @Override
    public List<String> getSessionLinks(Integer userId, Integer scheduledId) {
        Integer teacherId = getTeacherId(userId);
        if (!validateTeacherOwnsSession(teacherId, scheduledId)) {
            throw new IllegalArgumentException("Sesión no encontrada o no pertenece a este docente");
        }

        String sql = "SELECT urlarchivorefuerzoprogramado " +
                "FROM reforzamiento.tbrecursosrefuerzosprogramados " +
                "WHERE idrefuerzoprogramado = :scheduledId " +
                "AND (urlarchivorefuerzoprogramado LIKE 'virtual_link:%' OR urlarchivorefuerzoprogramado LIKE 'link:%') " +
                "ORDER BY idrecursorefuerzoprogramado DESC";

        List<String> rawLinks = getJdbcTemplate().queryForList(
                sql,
                new MapSqlParameterSource("scheduledId", scheduledId),
                String.class
        );
        
        List<String> cleanLinks = new ArrayList<>();
        for (String l : rawLinks) {
            if (l.startsWith("virtual_link:")) cleanLinks.add(l.replace("virtual_link:", "").trim());
            else if (l.startsWith("link:")) cleanLinks.add(l.replace("link:", "").trim());
            else cleanLinks.add(l);
        }
        return cleanLinks;
    }

    /**
     * PUT bulk attendance update for a scheduled session (uses idrefuerzoprogramado).
     */
    @Override
    @Transactional
    public TeacherActionResponseDTO updateSessionAttendance(Integer userId, Integer scheduledId,
                                                            List<AttendanceItemDTO> attendances) {
        if (!validateTeacherOwnsSession(getTeacherId(userId), scheduledId)) {
            return new TeacherActionResponseDTO(scheduledId, "ERROR",
                    "Sesión no encontrada o no pertenece a este docente");
        }
        for (AttendanceItemDTO item : attendances) {
            String updateSql = "UPDATE reforzamiento.tbasistenciasrefuerzos " +
                    "SET asistencia = :attended " +
                    "WHERE idrefuerzoprogramado = :scheduledId AND idparticipante = :participantId";
            MapSqlParameterSource p = new MapSqlParameterSource();
            p.addValue("attended", item.getAttended());
            p.addValue("scheduledId", scheduledId);
            p.addValue("participantId", item.getParticipantId());
            getJdbcTemplate().update(updateSql, p);
        }
        return new TeacherActionResponseDTO(scheduledId, "ATTENDANCE_UPDATED",
                "Asistencia actualizada correctamente");
    }

    /**
     * RF17: Register session result (creates tbrefuerzosrealizados record).
     * Returns the new performed reinforcement ID.
     */
    @Override
    @Transactional
    public Integer registerResult(Integer userId, Integer scheduledId, String observation, String duration) {
        Integer teacherId = getTeacherId(userId);
        if (!validateTeacherOwnsSession(teacherId, scheduledId)) {
            throw new IllegalArgumentException(
                    "Sesión no encontrada o no pertenece a este docente");
        }

        // Check if a result already exists for this session
        String checkSql = "SELECT COUNT(*) FROM reforzamiento.tbrefuerzosrealizados " +
                "WHERE idrefuerzoprogramado = :scheduledId";
        Integer count = getJdbcTemplate().queryForObject(checkSql,
                new MapSqlParameterSource("scheduledId", scheduledId), Integer.class);

        Integer performedId;
        if (count != null && count > 0) {
            // Update existing
            String updateSql = "UPDATE reforzamiento.tbrefuerzosrealizados " +
                    "SET observacion = :observation, duracion = :duration::time, estado = 'F' " +
                    "WHERE idrefuerzoprogramado = :scheduledId " +
                    "RETURNING idrefuerzorealizado";
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("observation", observation);
            params.addValue("duration", duration);
            params.addValue("scheduledId", scheduledId);
            performedId = getJdbcTemplate().queryForObject(updateSql, params, Integer.class);
        } else {
            // Insert new
            String insertSql = "INSERT INTO reforzamiento.tbrefuerzosrealizados " +
                    "(duracion, observacion, estado, idrefuerzoprogramado) " +
                    "VALUES (:duration::time, :observation, 'F', :scheduledId) " +
                    "RETURNING idrefuerzorealizado";
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("duration", duration);
            params.addValue("observation", observation);
            params.addValue("scheduledId", scheduledId);
            performedId = getJdbcTemplate().queryForObject(insertSql, params, Integer.class);
        }

        // Update session status to "Realizado" (id=3)
        Integer realizadoStatusId = getScheduledStatusId("Realizado");
        String updateStatusSql = "UPDATE reforzamiento.tbrefuerzosprogramados " +
                "SET idestadorefuerzoprogramado = :statusId " +
                "WHERE idrefuerzoprogramado = :scheduledId";
        MapSqlParameterSource statusParams = new MapSqlParameterSource();
        statusParams.addValue("statusId", realizadoStatusId);
        statusParams.addValue("scheduledId", scheduledId);
        getJdbcTemplate().update(updateStatusSql, statusParams);

        // Update reinforcement request status to "Completada"
        String getRequestIdSql = "SELECT idsolicitudrefuerzo " +
                "FROM reforzamiento.tbdetallesrefuerzosprogramadas " +
                "WHERE idrefuerzoprogramado = :scheduledId LIMIT 1";
        List<Integer> requestIds = getJdbcTemplate().queryForList(
                getRequestIdSql, new MapSqlParameterSource("scheduledId", scheduledId), Integer.class);
        if (!requestIds.isEmpty()) {
            String getCompletadaIdSql = "SELECT idestadosolicitudrefuerzo " +
                    "FROM reforzamiento.tbestadossolicitudesrefuerzos " +
                    "WHERE LOWER(nombreestado) = 'completada' LIMIT 1";
            List<Integer> completadaIds = getJdbcTemplate().queryForList(
                    getCompletadaIdSql, new MapSqlParameterSource(), Integer.class);
            if (!completadaIds.isEmpty()) {
                String updateRequestSql = "UPDATE reforzamiento.tbsolicitudesrefuerzos " +
                        "SET idestadosolicitudrefuerzo = :completadaId " +
                        "WHERE idsolicitudrefuerzo = :requestId";
                MapSqlParameterSource reqParams = new MapSqlParameterSource();
                reqParams.addValue("completadaId", completadaIds.get(0));
                reqParams.addValue("requestId", requestIds.get(0));
                getJdbcTemplate().update(updateRequestSql, reqParams);
            }
        }

        return performedId;
    }

    /**
     * RF17: Add a resource file URL to a scheduled reinforcement session.
     */
    @Override
    public TeacherActionResponseDTO addResource(Integer userId, Integer scheduledId, String fileUrl) {
        Integer teacherId = getTeacherId(userId);
        if (!validateTeacherOwnsSession(teacherId, scheduledId)) {
            return new TeacherActionResponseDTO(scheduledId, "ERROR",
                    "Sesión no encontrada o no pertenece a este docente");
        }

        String insertSql = "INSERT INTO reforzamiento.tbrecursosrefuerzosprogramados " +
                "(idrefuerzoprogramado, urlarchivorefuerzoprogramado) " +
                "VALUES (:scheduledId, :fileUrl)";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("scheduledId", scheduledId);
        params.addValue("fileUrl", fileUrl);
        getJdbcTemplate().update(insertSql, params);

        return new TeacherActionResponseDTO(scheduledId, "RESOURCE_ADDED",
                "Recurso adjuntado correctamente");
    }

    @Override
    public void deleteResource(Integer userId, Integer scheduledId, String fileUrl) {
        Integer teacherId = getTeacherId(userId);
        if (!validateTeacherOwnsSession(teacherId, scheduledId)) {
            throw new IllegalArgumentException("Sesión no encontrada o no pertenece a este docente");
        }

        String sql = "DELETE FROM reforzamiento.tbrecursosrefuerzosprogramados " +
                "WHERE idrefuerzoprogramado = :scheduledId AND urlarchivorefuerzoprogramado = :fileUrl";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("scheduledId", scheduledId);
        params.addValue("fileUrl", fileUrl);

        int rows = getJdbcTemplate().update(sql, params);
        if (rows == 0) {
            throw new IllegalArgumentException("El recurso no existe o no se pudo eliminar");
        }
    }

    /**
     * RF13: Register link for a scheduled session (CRUD).
     * Stored in tbrecursosrefuerzosprogramados with prefix 'link:'.
     */
    @Override
    @Transactional
    public TeacherActionResponseDTO addLink(Integer userId, Integer scheduledId, String url) {
        Integer teacherId = getTeacherId(userId);
        if (!validateTeacherOwnsSession(teacherId, scheduledId)) {
            return new TeacherActionResponseDTO(scheduledId, "ERROR",
                    "Sesión no encontrada o no pertenece a este docente");
        }

        String insertSql = "INSERT INTO reforzamiento.tbrecursosrefuerzosprogramados " +
                "(idrefuerzoprogramado, urlarchivorefuerzoprogramado) " +
                "VALUES (:scheduledId, :url)";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("scheduledId", scheduledId);
        params.addValue("url", "link:" + url);
        getJdbcTemplate().update(insertSql, params);

        return new TeacherActionResponseDTO(scheduledId, "LINK_ADDED",
                "Enlace agregado correctamente");
    }

    @Override
    @Transactional
    public void deleteLink(Integer userId, Integer scheduledId, String url) {
        Integer teacherId = getTeacherId(userId);
        if (!validateTeacherOwnsSession(teacherId, scheduledId)) {
            throw new IllegalArgumentException("Sesión no encontrada o no pertenece a este docente");
        }

        // Try deleting with both prefixes to be safe and backward compatible
        String sql = "DELETE FROM reforzamiento.tbrecursosrefuerzosprogramados " +
                "WHERE idrefuerzoprogramado = :scheduledId " +
                "AND (urlarchivorefuerzoprogramado = :linkUrl OR urlarchivorefuerzoprogramado = :virtualLinkUrl)";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("scheduledId", scheduledId);
        params.addValue("linkUrl", "link:" + url);
        params.addValue("virtualLinkUrl", "virtual_link:" + url);

        int rows = getJdbcTemplate().update(sql, params);
        if (rows == 0) {
            throw new IllegalArgumentException("El enlace no existe o no se pudo eliminar");
        }
    }

    /**
     * RF16: Mark attendance per participant for a performed session.
     */
    @Override
    @Transactional
    public TeacherActionResponseDTO markAttendance(Integer userId, Integer scheduledId, Integer performedId,
                                                   List<AttendanceItemDTO> attendances) {
        Integer teacherId = getTeacherId(userId);
        if (!validateTeacherOwnsSession(teacherId, scheduledId)) {
            return new TeacherActionResponseDTO(scheduledId, "ERROR",
                    "Sesión no encontrada o no pertenece a este docente");
        }

        for (AttendanceItemDTO item : attendances) {
            // Check if attendance record exists
            String checkSql = "SELECT COUNT(*) FROM reforzamiento.tbasistenciasrefuerzos " +
                    "WHERE idparticipante = :participantId AND idrefuerzorealizado = :performedId";
            MapSqlParameterSource checkParams = new MapSqlParameterSource();
            checkParams.addValue("participantId", item.getParticipantId());
            checkParams.addValue("performedId", performedId);
            Integer count = getJdbcTemplate().queryForObject(checkSql, checkParams, Integer.class);

            if (count != null && count > 0) {
                String updateSql = "UPDATE reforzamiento.tbasistenciasrefuerzos " +
                        "SET asistencia = :attended " +
                        "WHERE idparticipante = :participantId AND idrefuerzorealizado = :performedId";
                MapSqlParameterSource updateParams = new MapSqlParameterSource();
                updateParams.addValue("attended", item.getAttended());
                updateParams.addValue("participantId", item.getParticipantId());
                updateParams.addValue("performedId", performedId);
                getJdbcTemplate().update(updateSql, updateParams);
            } else {
                String insertSql = "INSERT INTO reforzamiento.tbasistenciasrefuerzos " +
                        "(asistencia, idparticipante, idrefuerzorealizado) " +
                        "VALUES (:attended, :participantId, :performedId)";
                MapSqlParameterSource insertParams = new MapSqlParameterSource();
                insertParams.addValue("attended", item.getAttended());
                insertParams.addValue("participantId", item.getParticipantId());
                insertParams.addValue("performedId", performedId);
                getJdbcTemplate().update(insertSql, insertParams);
            }
        }

        return new TeacherActionResponseDTO(performedId, "ATTENDANCE_MARKED",
                "Asistencia registrada correctamente");
    }
}
