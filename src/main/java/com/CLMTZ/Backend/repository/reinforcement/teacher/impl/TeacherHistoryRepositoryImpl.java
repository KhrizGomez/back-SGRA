package com.CLMTZ.Backend.repository.reinforcement.teacher.impl;

import com.CLMTZ.Backend.config.DynamicDataSourceService;
import com.CLMTZ.Backend.dto.reinforcement.teacher.AttendanceStudentDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherSessionHistoryDetailDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherSessionHistoryItemDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherSessionHistoryPageDTO;
import com.CLMTZ.Backend.repository.reinforcement.teacher.TeacherHistoryRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

@Repository
public class TeacherHistoryRepositoryImpl implements TeacherHistoryRepository {

    private final DynamicDataSourceService dynamicDataSourceService;

    public TeacherHistoryRepositoryImpl(DynamicDataSourceService dynamicDataSourceService) {
        this.dynamicDataSourceService = dynamicDataSourceService;
    }

    private NamedParameterJdbcTemplate getJdbcTemplate() {
        return dynamicDataSourceService.getJdbcTemplate();
    }

    /**
     * RF18: Teacher session history with subject, date, modality, duration, status.
     */
    @Override
    public TeacherSessionHistoryPageDTO getSessionHistory(Integer userId, Integer page, Integer size) {
        int offset = (page - 1) * size;

        String countSql = "SELECT COUNT(DISTINCT rp.idrefuerzoprogramado) " +
                "FROM reforzamiento.tbrefuerzosprogramados rp " +
                "JOIN reforzamiento.tbestadosrefuerzosprogramados est ON rp.idestadorefuerzoprogramado = est.idestadorefuerzoprogramado " +
                "JOIN reforzamiento.tbdetallesrefuerzosprogramadas d ON rp.idrefuerzoprogramado = d.idrefuerzoprogramado " +
                "JOIN reforzamiento.tbsolicitudesrefuerzos sr ON d.idsolicitudrefuerzo = sr.idsolicitudrefuerzo " +
                "JOIN academico.tbdocentes doc ON sr.iddocente = doc.iddocente " +
                "WHERE doc.idusuario = :userId " +
                "AND LOWER(est.estadorefuerzoprogramado) = 'realizado'";

        String dataSql = "SELECT DISTINCT ON (rp.idrefuerzoprogramado) " +
                "rp.idrefuerzoprogramado AS id_programado, " +
                "a.asignatura AS asignatura, " +
                "rp.fechaprogramadarefuerzo AS fecha_programada, " +
                "m.modalidad, " +
                "rp.duracionestimado AS duracion_estimada, " +
                "CONCAT(fh.horainicio::text, ' - ', fh.horariofin::text) AS franja_horaria, " +
                "est.estadorefuerzoprogramado AS estado, " +
                "ts.tiposesion AS tipo_sesion, " +
                "(SELECT COUNT(*) FROM reforzamiento.tbdetallesrefuerzosprogramadas dd " +
                " WHERE dd.idrefuerzoprogramado = rp.idrefuerzoprogramado) AS num_sesiones, " +
                "(SELECT COUNT(*) FROM reforzamiento.tbasistenciasrefuerzos att " +
                " WHERE att.idrefuerzoprogramado = rp.idrefuerzoprogramado) AS total_participantes, " +
                "(SELECT COUNT(*) FROM reforzamiento.tbasistenciasrefuerzos att " +
                " WHERE att.idrefuerzoprogramado = rp.idrefuerzoprogramado AND att.asistencia = TRUE) AS asistentes, " +
                "(SELECT COUNT(*) FROM reforzamiento.tbrecursosrefuerzosprogramados r " +
                " WHERE r.idrefuerzoprogramado = rp.idrefuerzoprogramado " +
                " AND r.urlarchivorefuerzoprogramado NOT LIKE 'virtual_link:%') AS num_recursos " +
                "FROM reforzamiento.tbrefuerzosprogramados rp " +
                "JOIN reforzamiento.tbestadosrefuerzosprogramados est ON rp.idestadorefuerzoprogramado = est.idestadorefuerzoprogramado " +
                "JOIN academico.tbmodalidades m ON rp.idmodalidad = m.idmodalidad " +
                "JOIN academico.tbfranjashorarias fh ON rp.idfranjahoraria = fh.idfranjahoraria " +
                "JOIN reforzamiento.tbtipossesiones ts ON rp.idtiposesion = ts.idtiposesion " +
                "JOIN reforzamiento.tbdetallesrefuerzosprogramadas d ON rp.idrefuerzoprogramado = d.idrefuerzoprogramado " +
                "JOIN reforzamiento.tbsolicitudesrefuerzos sr ON d.idsolicitudrefuerzo = sr.idsolicitudrefuerzo " +
                "JOIN academico.tbasignaturas a ON sr.idasignatura = a.idasignatura " +
                "JOIN academico.tbdocentes doc ON sr.iddocente = doc.iddocente " +
                "WHERE doc.idusuario = :userId " +
                "AND LOWER(est.estadorefuerzoprogramado) = 'realizado' " +
                "ORDER BY rp.idrefuerzoprogramado DESC, rp.fechaprogramadarefuerzo DESC " +
                "LIMIT :size OFFSET :offset";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("size", size);
        params.addValue("offset", offset);

        Long total = getJdbcTemplate().queryForObject(countSql, params, Long.class);
        List<TeacherSessionHistoryItemDTO> items = new ArrayList<>();

        getJdbcTemplate().query(dataSql, params, (rs) -> {
            TeacherSessionHistoryItemDTO item = new TeacherSessionHistoryItemDTO();
            item.setScheduledId(rs.getInt("id_programado"));
            item.setSubjectName(rs.getString("asignatura"));

            Date date = rs.getDate("fecha_programada");
            item.setScheduledDate(date != null ? date.toString() : null);

            item.setModality(rs.getString("modalidad"));

            Time duration = rs.getTime("duracion_estimada");
            item.setEstimatedDuration(duration != null ? duration.toString() : null);

            item.setTimeSlot(rs.getString("franja_horaria"));
            item.setStatusName(rs.getString("estado"));
            item.setSessionType(rs.getString("tipo_sesion"));
            item.setStudentCount(rs.getInt("num_sesiones"));
            int total_ = rs.getInt("total_participantes");
            int attended_ = rs.getInt("asistentes");
            item.setTotalParticipants(total_);
            item.setAttendedCount(attended_);
            item.setAttendancePercentage(total_ > 0 ? Math.round((attended_ * 100.0 / total_) * 10.0) / 10.0 : 0.0);
            item.setResourceCount(rs.getInt("num_recursos"));
            items.add(item);
        });

        return new TeacherSessionHistoryPageDTO(items, total != null ? total : 0L, page, size);
    }

    @Override
    public TeacherSessionHistoryDetailDTO getSessionHistoryDetail(Integer userId, Integer scheduledId) {

        // 1. Base session info (also verifies teacher ownership)
        String baseSql = "SELECT DISTINCT ON (rp.idrefuerzoprogramado) " +
                "rp.idrefuerzoprogramado AS scheduled_id, " +
                "a.asignatura AS subject_name, " +
                "rp.fechaprogramadarefuerzo AS scheduled_date, " +
                "m.modalidad, " +
                "CONCAT(fh.horainicio::text, ' - ', fh.horariofin::text) AS franja_horaria, " +
                "ts.tiposesion AS session_type, " +
                "est.estadorefuerzoprogramado AS status_name, " +
                "rp.duracionestimado AS duracion_estimada " +
                "FROM reforzamiento.tbrefuerzosprogramados rp " +
                "JOIN reforzamiento.tbestadosrefuerzosprogramados est ON rp.idestadorefuerzoprogramado = est.idestadorefuerzoprogramado " +
                "JOIN academico.tbmodalidades m ON rp.idmodalidad = m.idmodalidad " +
                "JOIN academico.tbfranjashorarias fh ON rp.idfranjahoraria = fh.idfranjahoraria " +
                "JOIN reforzamiento.tbtipossesiones ts ON rp.idtiposesion = ts.idtiposesion " +
                "JOIN reforzamiento.tbdetallesrefuerzosprogramadas d ON rp.idrefuerzoprogramado = d.idrefuerzoprogramado " +
                "JOIN reforzamiento.tbsolicitudesrefuerzos sr ON d.idsolicitudrefuerzo = sr.idsolicitudrefuerzo " +
                "JOIN academico.tbasignaturas a ON sr.idasignatura = a.idasignatura " +
                "JOIN academico.tbdocentes doc ON sr.iddocente = doc.iddocente " +
                "WHERE rp.idrefuerzoprogramado = :scheduledId AND doc.idusuario = :userId";

        MapSqlParameterSource baseParams = new MapSqlParameterSource();
        baseParams.addValue("scheduledId", scheduledId);
        baseParams.addValue("userId", userId);

        List<TeacherSessionHistoryDetailDTO> baseResult = new ArrayList<>();
        getJdbcTemplate().query(baseSql, baseParams, rs -> {
            TeacherSessionHistoryDetailDTO dto = new TeacherSessionHistoryDetailDTO();
            dto.setScheduledId(rs.getInt("scheduled_id"));
            dto.setSubjectName(rs.getString("subject_name"));
            Date d = rs.getDate("scheduled_date");
            dto.setScheduledDate(d != null ? d.toString() : null);
            dto.setModality(rs.getString("modalidad"));
            dto.setTimeSlot(rs.getString("franja_horaria"));
            dto.setSessionType(rs.getString("session_type"));
            dto.setStatusName(rs.getString("status_name"));
            Time dur = rs.getTime("duracion_estimada");
            dto.setEstimatedDuration(dur != null ? dur.toString().substring(0, 5) : null);
            baseResult.add(dto);
        });

        if (baseResult.isEmpty()) {
            throw new RuntimeException("Sesi\u00f3n no encontrada o no pertenece a este docente");
        }

        TeacherSessionHistoryDetailDTO detail = baseResult.get(0);

        // 2. Performed info (observation + actual duration)
        String performedSql = "SELECT observacion, duracion " +
                "FROM reforzamiento.tbrefuerzosrealizados " +
                "WHERE idrefuerzoprogramado = :scheduledId " +
                "ORDER BY idrefuerzorealizado DESC LIMIT 1";
        getJdbcTemplate().query(performedSql, new MapSqlParameterSource("scheduledId", scheduledId), rs -> {
            detail.setObservation(rs.getString("observacion"));
            Time actualDur = rs.getTime("duracion");
            detail.setActualDuration(actualDur != null ? actualDur.toString().substring(0, 5) : null);
        });

        // 3. Attendance per student
        String attendanceSql = "SELECT p.idparticipante, " +
                "CONCAT(u.nombres, ' ', u.apellidos) AS student_name, " +
                "a.asistencia " +
                "FROM reforzamiento.tbasistenciasrefuerzos a " +
                "JOIN reforzamiento.tbparticipantes p ON a.idparticipante = p.idparticipante " +
                "JOIN academico.tbestudiantes e ON p.idestudiante = e.idestudiante " +
                "JOIN general.tbusuarios u ON e.idusuario = u.idusuario " +
                "WHERE a.idrefuerzoprogramado = :scheduledId " +
                "ORDER BY u.apellidos, u.nombres";
        List<AttendanceStudentDTO> attendanceList = new ArrayList<>();
        getJdbcTemplate().query(attendanceSql, new MapSqlParameterSource("scheduledId", scheduledId), rs -> {
            attendanceList.add(new AttendanceStudentDTO(
                    rs.getInt("idparticipante"),
                    rs.getString("student_name"),
                    rs.getBoolean("asistencia")));
        });
        detail.setAttendance(attendanceList);
        int total = attendanceList.size();
        int attended = (int) attendanceList.stream().filter(AttendanceStudentDTO::getAttended).count();
        detail.setTotalParticipants(total);
        detail.setAttendedCount(attended);
        detail.setAttendancePercentage(total > 0 ? Math.round((attended * 100.0 / total) * 10.0) / 10.0 : 0.0);

        // 4. File resources (exclude virtual_link entries)
        String resourcesSql = "SELECT urlarchivorefuerzoprogramado " +
                "FROM reforzamiento.tbrecursosrefuerzosprogramados " +
                "WHERE idrefuerzoprogramado = :scheduledId " +
                "AND urlarchivorefuerzoprogramado NOT LIKE 'virtual_link:%'";
        List<String> resources = getJdbcTemplate().queryForList(
                resourcesSql, new MapSqlParameterSource("scheduledId", scheduledId), String.class);
        detail.setResources(resources);

        // 5. Virtual link
        String virtualLinkSql = "SELECT urlarchivorefuerzoprogramado " +
                "FROM reforzamiento.tbrecursosrefuerzosprogramados " +
                "WHERE idrefuerzoprogramado = :scheduledId " +
                "AND urlarchivorefuerzoprogramado LIKE 'virtual_link:%' LIMIT 1";
        List<String> vl = getJdbcTemplate().queryForList(
                virtualLinkSql, new MapSqlParameterSource("scheduledId", scheduledId), String.class);
        if (!vl.isEmpty()) {
            detail.setVirtualLink(vl.get(0).replace("virtual_link:", "").trim());
        }

        return detail;
    }
}
