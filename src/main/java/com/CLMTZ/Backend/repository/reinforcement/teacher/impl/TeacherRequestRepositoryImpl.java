package com.CLMTZ.Backend.repository.reinforcement.teacher.impl;

import com.CLMTZ.Backend.config.DynamicDataSourceService;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherActionResponseDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherRequestItemDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherRequestsPageDTO;
import com.CLMTZ.Backend.repository.reinforcement.teacher.TeacherRequestRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class TeacherRequestRepositoryImpl implements TeacherRequestRepository {

    private final DynamicDataSourceService dynamicDataSourceService;

    public TeacherRequestRepositoryImpl(DynamicDataSourceService dynamicDataSourceService) {
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

    private Integer getRequestStatusId(String statusName) {
        String sql = "SELECT idestadosolicitudrefuerzo FROM reforzamiento.tbestadossolicitudesrefuerzos " +
                "WHERE LOWER(nombreestado) = LOWER(:statusName) LIMIT 1";
        MapSqlParameterSource params = new MapSqlParameterSource("statusName", statusName);
        return getJdbcTemplate().queryForObject(sql, params, Integer.class);
    }

    private Integer getScheduledStatusId(String statusName) {
        String sql = "SELECT idestadorefuerzoprogramado FROM reforzamiento.tbestadosrefuerzosprogramados " +
                "WHERE LOWER(estadorefuerzoprogramado) LIKE LOWER(:statusName) || '%' LIMIT 1";
        MapSqlParameterSource params = new MapSqlParameterSource("statusName", statusName);
        return getJdbcTemplate().queryForObject(sql, params, Integer.class);
    }

    @Override
    public TeacherRequestsPageDTO getIncomingRequests(Integer userId, Integer statusId, Integer page, Integer size) {
        Integer teacherId = getTeacherId(userId);
        int offset = (page - 1) * size;

        String countSql = "SELECT COUNT(*) FROM reforzamiento.tbsolicitudesrefuerzos sr " +
                "WHERE sr.iddocente = :teacherId " +
                "AND (:statusId IS NULL OR sr.idestadosolicitudrefuerzo = :statusId) " +
                "AND sr.idestadosolicitudrefuerzo IN (" +
                "  SELECT idestadosolicitudrefuerzo FROM reforzamiento.tbestadossolicitudesrefuerzos " +
                "  WHERE estado = TRUE)";

        String dataSql = "SELECT sr.idsolicitudrefuerzo, " +
                "CONCAT(u.nombres, ' ', u.apellidos) AS estudiante, " +
                "a.asignatura AS asignatura, " +
                "ts.tiposesion AS tipo_sesion, " +
                "sr.motivo, " +
                "est.nombreestado AS estado, " +
                "est.idestadosolicitudrefuerzo AS estado_id, " +
                "sr.fechahoracreacion, " +
                "sr.idtiposesion, " +
                "(SELECT COUNT(*) FROM reforzamiento.tbparticipantes p " +
                " WHERE p.idsolicitudrefuerzo = sr.idsolicitudrefuerzo) AS participantes " +
                "FROM reforzamiento.tbsolicitudesrefuerzos sr " +
                "JOIN academico.tbestudiantes e ON sr.idestudiante = e.idestudiante " +
                "JOIN general.tbusuarios u ON e.idusuario = u.idusuario " +
                "JOIN academico.tbasignaturas a ON sr.idasignatura = a.idasignatura " +
                "JOIN reforzamiento.tbtipossesiones ts ON sr.idtiposesion = ts.idtiposesion " +
                "JOIN reforzamiento.tbestadossolicitudesrefuerzos est ON sr.idestadosolicitudrefuerzo = est.idestadosolicitudrefuerzo " +
                "WHERE sr.iddocente = :teacherId " +
                "AND (:statusId IS NULL OR sr.idestadosolicitudrefuerzo = :statusId) " +
                "AND est.estado = TRUE " +
                "ORDER BY sr.fechahoracreacion DESC " +
                "LIMIT :size OFFSET :offset";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("teacherId", teacherId);
        params.addValue("statusId", statusId);
        params.addValue("size", size);
        params.addValue("offset", offset);

        Long total = getJdbcTemplate().queryForObject(countSql, params, Long.class);
        List<TeacherRequestItemDTO> items = new ArrayList<>();

        getJdbcTemplate().query(dataSql, params, (rs) -> {
            TeacherRequestItemDTO item = new TeacherRequestItemDTO();
            item.setRequestId(rs.getInt("idsolicitudrefuerzo"));
            item.setStudentName(rs.getString("estudiante"));
            item.setSubjectName(rs.getString("asignatura"));
            item.setSessionType(rs.getString("tipo_sesion"));
            item.setReason(rs.getString("motivo"));
            item.setStatusName(rs.getString("estado"));
            item.setStatusId(rs.getInt("estado_id"));
            Timestamp ts = rs.getTimestamp("fechahoracreacion");
            item.setCreatedAt(ts != null ? ts.toInstant().toString() : null);
            int sessionTypeId = rs.getInt("idtiposesion");
            item.setIsGroupal(sessionTypeId == 2); // 2 = grupal typically
            item.setParticipantCount(rs.getInt("participantes"));
            items.add(item);
        });

        return new TeacherRequestsPageDTO(items, total != null ? total : 0L, page, size);
    }

    @Override
    @Transactional
    public TeacherActionResponseDTO acceptRequest(Integer userId, Integer requestId, String scheduledDate,
                                                   Integer timeSlotId, Integer modalityId, String estimatedDuration,
                                                   String reason, Integer workAreaId) {
        Integer teacherId = getTeacherId(userId);

        // Verify ownership and status (must be Pendiente)
        Integer pendienteId = getRequestStatusId("Pendiente");
        String checkSql = "SELECT COUNT(*) FROM reforzamiento.tbsolicitudesrefuerzos " +
                "WHERE idsolicitudrefuerzo = :requestId AND iddocente = :teacherId " +
                "AND idestadosolicitudrefuerzo = :pendienteId";
        MapSqlParameterSource checkParams = new MapSqlParameterSource();
        checkParams.addValue("requestId", requestId);
        checkParams.addValue("teacherId", teacherId);
        checkParams.addValue("pendienteId", pendienteId);
        Integer count = getJdbcTemplate().queryForObject(checkSql, checkParams, Integer.class);
        if (count == null || count == 0) {
            return new TeacherActionResponseDTO(requestId, "ERROR",
                    "Solicitud no encontrada, no pertenece a este docente o no está en estado Pendiente");
        }

        // Get status IDs
        Integer aceptadaId = getRequestStatusId("Aceptada");
        Integer scheduledPendienteId = getScheduledStatusId("Pendiente");

        // Get the session type from the request
        String sessionTypeSql = "SELECT idtiposesion FROM reforzamiento.tbsolicitudesrefuerzos " +
                "WHERE idsolicitudrefuerzo = :requestId";
        Integer sessionTypeId = getJdbcTemplate().queryForObject(sessionTypeSql,
                new MapSqlParameterSource("requestId", requestId), Integer.class);

        // Update request status to Aceptada
        String updateRequestSql = "UPDATE reforzamiento.tbsolicitudesrefuerzos " +
                "SET idestadosolicitudrefuerzo = :aceptadaId " +
                "WHERE idsolicitudrefuerzo = :requestId";
        MapSqlParameterSource updateParams = new MapSqlParameterSource();
        updateParams.addValue("aceptadaId", aceptadaId);
        updateParams.addValue("requestId", requestId);
        getJdbcTemplate().update(updateRequestSql, updateParams);

        // Insert scheduled reinforcement
        String insertScheduledSql = "INSERT INTO reforzamiento.tbrefuerzosprogramados " +
                "(idtiposesion, idmodalidad, idfranjahoraria, fechaprogramadarefuerzo, duracionestimado, " +
                "motivo, fechacreacion, idestadorefuerzoprogramado) " +
                "VALUES (:sessionTypeId, :modalityId, :timeSlotId, :scheduledDate::date, " +
                ":estimatedDuration::time, :reason, NOW(), :statusId) " +
                "RETURNING idrefuerzoprogramado";
        MapSqlParameterSource insertParams = new MapSqlParameterSource();
        insertParams.addValue("sessionTypeId", sessionTypeId);
        insertParams.addValue("modalityId", modalityId);
        insertParams.addValue("timeSlotId", timeSlotId);
        insertParams.addValue("scheduledDate", scheduledDate);
        insertParams.addValue("estimatedDuration", estimatedDuration);
        insertParams.addValue("reason", reason);
        insertParams.addValue("statusId", scheduledPendienteId);

        Integer scheduledId = getJdbcTemplate().queryForObject(insertScheduledSql, insertParams, Integer.class);

        // Link request to scheduled reinforcement
        String insertDetailSql = "INSERT INTO reforzamiento.tbdetallesrefuerzosprogramadas " +
                "(idsolicitudrefuerzo, idrefuerzoprogramado, estado) " +
                "VALUES (:requestId, :scheduledId, TRUE)";
        MapSqlParameterSource detailParams = new MapSqlParameterSource();
        detailParams.addValue("requestId", requestId);
        detailParams.addValue("scheduledId", scheduledId);
        getJdbcTemplate().update(insertDetailSql, detailParams);

        // If in-person modality and workAreaId provided, insert on-site record
        if (workAreaId != null) {
            String workAreaTypeSql = "SELECT idtipoareatrabajo FROM reforzamiento.tbareastrabajos " +
                    "WHERE idareatrabajo = :workAreaId";
            Integer workAreaTypeId = getJdbcTemplate().queryForObject(workAreaTypeSql,
                    new MapSqlParameterSource("workAreaId", workAreaId), Integer.class);

            String insertPresencialSql = "INSERT INTO reforzamiento.tbrefuerzospresenciales " +
                    "(idrefuerzoprogramado, idareatrabajo, idtipoareatrabajo, estado) " +
                    "VALUES (:scheduledId, :workAreaId, :workAreaTypeId, TRUE)";
            MapSqlParameterSource presencialParams = new MapSqlParameterSource();
            presencialParams.addValue("scheduledId", scheduledId);
            presencialParams.addValue("workAreaId", workAreaId);
            presencialParams.addValue("workAreaTypeId", workAreaTypeId);
            getJdbcTemplate().update(insertPresencialSql, presencialParams);
        }

        return new TeacherActionResponseDTO(scheduledId, "ACCEPTED",
                "Solicitud aceptada y sesión programada correctamente");
    }

    @Override
    @Transactional
    public TeacherActionResponseDTO rejectRequest(Integer userId, Integer requestId, String reason) {
        Integer teacherId = getTeacherId(userId);
        Integer pendienteId = getRequestStatusId("Pendiente");

        // Verify ownership and status
        String checkSql = "SELECT COUNT(*) FROM reforzamiento.tbsolicitudesrefuerzos " +
                "WHERE idsolicitudrefuerzo = :requestId AND iddocente = :teacherId " +
                "AND idestadosolicitudrefuerzo = :pendienteId";
        MapSqlParameterSource checkParams = new MapSqlParameterSource();
        checkParams.addValue("requestId", requestId);
        checkParams.addValue("teacherId", teacherId);
        checkParams.addValue("pendienteId", pendienteId);
        Integer count = getJdbcTemplate().queryForObject(checkSql, checkParams, Integer.class);
        if (count == null || count == 0) {
            return new TeacherActionResponseDTO(requestId, "ERROR",
                    "Solicitud no encontrada, no pertenece a este docente o no está en estado Pendiente");
        }

        Integer rechazadaId = getRequestStatusId("Rechazada");
        String updateSql = "UPDATE reforzamiento.tbsolicitudesrefuerzos " +
                "SET idestadosolicitudrefuerzo = :rechazadaId " +
                "WHERE idsolicitudrefuerzo = :requestId";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("rechazadaId", rechazadaId);
        params.addValue("requestId", requestId);
        getJdbcTemplate().update(updateSql, params);

        return new TeacherActionResponseDTO(requestId, "REJECTED", "Solicitud rechazada");
    }

    @Override
    @Transactional
    public TeacherActionResponseDTO rescheduleRequest(Integer userId, Integer requestId, String scheduledDate,
                                                       Integer timeSlotId, Integer modalityId, String estimatedDuration,
                                                       String reason, Integer workAreaId) {
        Integer teacherId = getTeacherId(userId);
        Integer aceptadaId = getRequestStatusId("Aceptada");

        // Verify ownership and status (must be Aceptada)
        String checkSql = "SELECT COUNT(*) FROM reforzamiento.tbsolicitudesrefuerzos " +
                "WHERE idsolicitudrefuerzo = :requestId AND iddocente = :teacherId " +
                "AND idestadosolicitudrefuerzo = :aceptadaId";
        MapSqlParameterSource checkParams = new MapSqlParameterSource();
        checkParams.addValue("requestId", requestId);
        checkParams.addValue("teacherId", teacherId);
        checkParams.addValue("aceptadaId", aceptadaId);
        Integer count = getJdbcTemplate().queryForObject(checkSql, checkParams, Integer.class);
        if (count == null || count == 0) {
            return new TeacherActionResponseDTO(requestId, "ERROR",
                    "Solicitud no encontrada, no pertenece a este docente o no está en estado Aceptada");
        }

        // Find the scheduled reinforcement linked to this request
        String findScheduledSql = "SELECT d.idrefuerzoprogramado " +
                "FROM reforzamiento.tbdetallesrefuerzosprogramadas d " +
                "WHERE d.idsolicitudrefuerzo = :requestId AND d.estado = TRUE LIMIT 1";
        List<Integer> scheduledIds = getJdbcTemplate().queryForList(findScheduledSql,
                new MapSqlParameterSource("requestId", requestId), Integer.class);

        if (scheduledIds.isEmpty()) {
            return new TeacherActionResponseDTO(requestId, "ERROR",
                    "No se encontró sesión programada para esta solicitud");
        }
        Integer scheduledId = scheduledIds.get(0);

        // Update scheduled reinforcement
        String updateSql = "UPDATE reforzamiento.tbrefuerzosprogramados " +
                "SET idfranjahoraria = :timeSlotId, idmodalidad = :modalityId, " +
                "fechaprogramadarefuerzo = :scheduledDate::date, " +
                "duracionestimado = :estimatedDuration::time, motivo = :reason " +
                "WHERE idrefuerzoprogramado = :scheduledId";
        MapSqlParameterSource updateParams = new MapSqlParameterSource();
        updateParams.addValue("timeSlotId", timeSlotId);
        updateParams.addValue("modalityId", modalityId);
        updateParams.addValue("scheduledDate", scheduledDate);
        updateParams.addValue("estimatedDuration", estimatedDuration);
        updateParams.addValue("reason", reason);
        updateParams.addValue("scheduledId", scheduledId);
        getJdbcTemplate().update(updateSql, updateParams);

        // Update on-site reinforcement record if workAreaId provided
        if (workAreaId != null) {
            String checkPresencialSql = "SELECT COUNT(*) FROM reforzamiento.tbrefuerzospresenciales " +
                    "WHERE idrefuerzoprogramado = :scheduledId";
            Integer presencialCount = getJdbcTemplate().queryForObject(checkPresencialSql,
                    new MapSqlParameterSource("scheduledId", scheduledId), Integer.class);

            String workAreaTypeSql = "SELECT idtipoareatrabajo FROM reforzamiento.tbareastrabajos " +
                    "WHERE idareatrabajo = :workAreaId";
            Integer workAreaTypeId = getJdbcTemplate().queryForObject(workAreaTypeSql,
                    new MapSqlParameterSource("workAreaId", workAreaId), Integer.class);

            if (presencialCount != null && presencialCount > 0) {
                String updatePresencialSql = "UPDATE reforzamiento.tbrefuerzospresenciales " +
                        "SET idareatrabajo = :workAreaId, idtipoareatrabajo = :workAreaTypeId " +
                        "WHERE idrefuerzoprogramado = :scheduledId";
                MapSqlParameterSource presencialParams = new MapSqlParameterSource();
                presencialParams.addValue("workAreaId", workAreaId);
                presencialParams.addValue("workAreaTypeId", workAreaTypeId);
                presencialParams.addValue("scheduledId", scheduledId);
                getJdbcTemplate().update(updatePresencialSql, presencialParams);
            } else {
                String insertPresencialSql = "INSERT INTO reforzamiento.tbrefuerzospresenciales " +
                        "(idrefuerzoprogramado, idareatrabajo, idtipoareatrabajo, estado) " +
                        "VALUES (:scheduledId, :workAreaId, :workAreaTypeId, TRUE)";
                MapSqlParameterSource presencialParams = new MapSqlParameterSource();
                presencialParams.addValue("scheduledId", scheduledId);
                presencialParams.addValue("workAreaId", workAreaId);
                presencialParams.addValue("workAreaTypeId", workAreaTypeId);
                getJdbcTemplate().update(insertPresencialSql, presencialParams);
            }
        }

        return new TeacherActionResponseDTO(scheduledId, "RESCHEDULED",
                "Sesión reprogramada correctamente");
    }

    @Override
    @Transactional
    public TeacherActionResponseDTO cancelSession(Integer userId, Integer scheduledId, String reason) {
        Integer teacherId = getTeacherId(userId);

        // Verify the teacher owns at least one request linked to this session
        String checkSql = "SELECT COUNT(*) " +
                "FROM reforzamiento.tbdetallesrefuerzosprogramadas d " +
                "JOIN reforzamiento.tbsolicitudesrefuerzos sr ON d.idsolicitudrefuerzo = sr.idsolicitudrefuerzo " +
                "WHERE d.idrefuerzoprogramado = :scheduledId AND sr.iddocente = :teacherId";
        MapSqlParameterSource checkParams = new MapSqlParameterSource();
        checkParams.addValue("scheduledId", scheduledId);
        checkParams.addValue("teacherId", teacherId);
        Integer count = getJdbcTemplate().queryForObject(checkSql, checkParams, Integer.class);
        if (count == null || count == 0) {
            return new TeacherActionResponseDTO(scheduledId, "ERROR",
                    "Sesión no encontrada o no pertenece a este docente");
        }

        // Get cancelled status ID for scheduled reinforcement
        Integer canceladaScheduledId = getScheduledStatusId("Cancelada");

        String updateSql = "UPDATE reforzamiento.tbrefuerzosprogramados " +
                "SET idestadorefuerzoprogramado = :canceladaId " +
                "WHERE idrefuerzoprogramado = :scheduledId";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("canceladaId", canceladaScheduledId);
        params.addValue("scheduledId", scheduledId);
        getJdbcTemplate().update(updateSql, params);

        // Also cancel linked requests
        Integer canceladaSolicitudId = getRequestStatusId("Cancelada");
        String updateRequestsSql = "UPDATE reforzamiento.tbsolicitudesrefuerzos " +
                "SET idestadosolicitudrefuerzo = :canceladaId " +
                "WHERE idsolicitudrefuerzo IN (" +
                "  SELECT d.idsolicitudrefuerzo FROM reforzamiento.tbdetallesrefuerzosprogramadas d " +
                "  WHERE d.idrefuerzoprogramado = :scheduledId)";
        MapSqlParameterSource requestParams = new MapSqlParameterSource();
        requestParams.addValue("canceladaId", canceladaSolicitudId);
        requestParams.addValue("scheduledId", scheduledId);
        getJdbcTemplate().update(updateRequestsSql, requestParams);

        return new TeacherActionResponseDTO(scheduledId, "CANCELLED",
                "Sesión cancelada correctamente");
    }
}
