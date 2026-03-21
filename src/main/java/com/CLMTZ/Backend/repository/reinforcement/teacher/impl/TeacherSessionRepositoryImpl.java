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

    private TeacherActionResponseDTO mapActionResponse(MapSqlParameterSource params, String sql) {
        List<TeacherActionResponseDTO> rows = getJdbcTemplate().query(sql, params, (rs, rowNum) ->
                new TeacherActionResponseDTO(
                        rs.getInt("entity_id"),
                        rs.getString("status"),
                        rs.getString("message")));

        if (rows.isEmpty()) {
            throw new RuntimeException("No se obtuvo respuesta desde la función de base de datos");
        }
        return rows.get(0);
    }

    private String toAttendanceJson(List<AttendanceItemDTO> attendances) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < attendances.size(); i++) {
            AttendanceItemDTO item = attendances.get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"participantid\":")
                    .append(item.getParticipantId())
                    .append(",\"attended\":")
                    .append(Boolean.TRUE.equals(item.getAttended()))
                    .append('}');
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * Returns sessions with status 'Espera espacio', 'Reprogramado' or 'Programado' for the authenticated teacher.
     */
    @Override
    public List<TeacherActiveSessionItemDTO> getActiveSessions(Integer userId) {
        String sql = "SELECT * FROM reforzamiento.fn_sl_teacher_active_sessions(:userId)";

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
        List<ParticipantAttendanceDTO> result = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("scheduledId", scheduledId);
        getJdbcTemplate().query(
                "SELECT * FROM reforzamiento.fn_sl_teacher_session_attendance(:userId, :scheduledId)",
                params,
                rs -> {
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
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("scheduledId", scheduledId);
        return getJdbcTemplate().queryForList(
                "SELECT * FROM reforzamiento.fn_sl_teacher_session_resources(:userId, :scheduledId)",
                params,
                String.class);
    }

    @Override
    public List<String> getSessionRequestResources(Integer userId, Integer scheduledId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("scheduledId", scheduledId);
        return getJdbcTemplate().queryForList(
                "SELECT * FROM reforzamiento.fn_sl_teacher_session_request_resources(:userId, :scheduledId)",
                params,
                String.class);
    }

    @Override
    public List<String> getSessionLinks(Integer userId, Integer scheduledId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("scheduledId", scheduledId);
        return getJdbcTemplate().queryForList(
                "SELECT * FROM reforzamiento.fn_sl_teacher_session_links(:userId, :scheduledId)",
                params,
                String.class);
    }

    /**
     * PUT bulk attendance update for a scheduled session (uses idrefuerzoprogramado).
     */
    @Override
    @Transactional
    public TeacherActionResponseDTO updateSessionAttendance(Integer userId, Integer scheduledId,
                                                            List<AttendanceItemDTO> attendances) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("scheduledId", scheduledId);
        params.addValue("attendances", toAttendanceJson(attendances));
        return mapActionResponse(
                params,
                "SELECT * FROM reforzamiento.fn_tx_teacher_update_session_attendance(" +
                        ":userId, :scheduledId, CAST(:attendances AS jsonb))");
    }

    /**
     * RF17: Register session result (creates tbrefuerzosrealizados record).
     * Returns the new performed reinforcement ID.
     */
    @Override
    @Transactional
    public Integer registerResult(Integer userId, Integer scheduledId, String observation, String duration) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("scheduledId", scheduledId);
        params.addValue("observation", observation);
        params.addValue("duration", duration);
        return getJdbcTemplate().queryForObject(
                "SELECT reforzamiento.fn_tx_teacher_register_result(:userId, :scheduledId, :observation, :duration)",
                params,
                Integer.class);
    }

    /**
     * RF17: Add a resource file URL to a scheduled reinforcement session.
     */
    @Override
    public TeacherActionResponseDTO addResource(Integer userId, Integer scheduledId, String fileUrl) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("scheduledId", scheduledId);
        params.addValue("fileUrl", fileUrl);
        return mapActionResponse(
                params,
                "SELECT * FROM reforzamiento.fn_tx_teacher_add_resource(:userId, :scheduledId, :fileUrl)");
    }

    @Override
    public void deleteResource(Integer userId, Integer scheduledId, String fileUrl) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("scheduledId", scheduledId);
        params.addValue("fileUrl", fileUrl);

        Boolean deleted = getJdbcTemplate().queryForObject(
                "SELECT reforzamiento.fn_tx_teacher_delete_resource(:userId, :scheduledId, :fileUrl)",
                params,
                Boolean.class);
        if (deleted == null || !deleted) {
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
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("scheduledId", scheduledId);
        params.addValue("url", "link:" + url);
        return mapActionResponse(
                params,
                "SELECT * FROM reforzamiento.fn_tx_teacher_add_link(:userId, :scheduledId, :url)");
    }

    @Override
    @Transactional
    public void deleteLink(Integer userId, Integer scheduledId, String url) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("scheduledId", scheduledId);
        params.addValue("url", url);

        Boolean deleted = getJdbcTemplate().queryForObject(
                "SELECT reforzamiento.fn_tx_teacher_delete_link(:userId, :scheduledId, :url)",
                params,
                Boolean.class);
        if (deleted == null || !deleted) {
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
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("scheduledId", scheduledId);
        params.addValue("performedId", performedId);
        params.addValue("attendances", toAttendanceJson(attendances));
        return mapActionResponse(
                params,
                "SELECT * FROM reforzamiento.fn_tx_teacher_mark_attendance(" +
                        ":userId, :scheduledId, :performedId, CAST(:attendances AS jsonb))");
    }
}
