package com.CLMTZ.Backend.repository.reinforcement.teacher.impl;

import com.CLMTZ.Backend.config.DynamicDataSourceService;
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
                "JOIN reforzamiento.tbdetallesrefuerzosprogramadas d ON rp.idrefuerzoprogramado = d.idrefuerzoprogramado " +
                "JOIN reforzamiento.tbsolicitudesrefuerzos sr ON d.idsolicitudrefuerzo = sr.idsolicitudrefuerzo " +
                "JOIN academico.tbdocentes doc ON sr.iddocente = doc.iddocente " +
                "WHERE doc.idusuario = :userId";

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
                " WHERE dd.idrefuerzoprogramado = rp.idrefuerzoprogramado) AS num_sesiones " +
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
            items.add(item);
        });

        return new TeacherSessionHistoryPageDTO(items, total != null ? total : 0L, page, size);
    }
}
