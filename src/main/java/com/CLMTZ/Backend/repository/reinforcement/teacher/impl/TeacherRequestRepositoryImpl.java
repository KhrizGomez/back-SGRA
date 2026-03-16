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
                List<Integer> rows = getJdbcTemplate().queryForList(sql, params, Integer.class);
                if (rows.isEmpty()) {
                        throw new RuntimeException("No se encontró docente activo para el usuario " + userId);
                }
                return rows.get(0);
        }

        private Integer getRequestStatusId(String statusName) {
                String sql = "SELECT idestadosolicitudrefuerzo FROM reforzamiento.tbestadossolicitudesrefuerzos " +
                                "WHERE LOWER(nombreestado) = LOWER(:statusName) LIMIT 1";
                MapSqlParameterSource params = new MapSqlParameterSource("statusName", statusName);
                List<Integer> rows = getJdbcTemplate().queryForList(sql, params, Integer.class);
                if (rows.isEmpty()) {
                        throw new RuntimeException("Estado de solicitud '" + statusName + "' no encontrado en tbestadossolicitudesrefuerzos");
                }
                return rows.get(0);
        }

        private Integer getScheduledStatusId(String statusName) {
                String sql = "SELECT idestadorefuerzoprogramado FROM reforzamiento.tbestadosrefuerzosprogramados " +
                                "WHERE LOWER(estadorefuerzoprogramado) = LOWER(:statusName) LIMIT 1";
                MapSqlParameterSource params = new MapSqlParameterSource("statusName", statusName);
                List<Integer> rows = getJdbcTemplate().queryForList(sql, params, Integer.class);
                if (rows.isEmpty()) {
                        throw new RuntimeException("Estado de refuerzo programado '" + statusName + "' no encontrado en tbestadosrefuerzosprogramados");
                }
                return rows.get(0);
        }

        @Override
        public TeacherRequestsPageDTO getIncomingRequests(Integer userId, Integer statusId, Integer page,
                        Integer size) {
                Integer teacherId = getTeacherId(userId);
                int offset = (page - 1) * size;

                String statusFilter = statusId != null ? "AND sr.idestadosolicitudrefuerzo = :statusId " : "";

                String countSql = "SELECT COUNT(*) FROM reforzamiento.tbsolicitudesrefuerzos sr " +
                                "JOIN reforzamiento.tbestadossolicitudesrefuerzos est ON sr.idestadosolicitudrefuerzo = est.idestadosolicitudrefuerzo " +
                                "WHERE sr.iddocente = :teacherId " +
                                statusFilter;

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
                                "JOIN reforzamiento.tbestadossolicitudesrefuerzos est ON sr.idestadosolicitudrefuerzo = est.idestadosolicitudrefuerzo "
                                +
                                "WHERE sr.iddocente = :teacherId " +
                                statusFilter +
                                "ORDER BY sr.fechahoracreacion DESC " +
                                "LIMIT :size OFFSET :offset";

                MapSqlParameterSource params = new MapSqlParameterSource();
                params.addValue("teacherId", teacherId);
                if (statusId != null) {
                        params.addValue("statusId", statusId);
                }
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
                        String reason, Integer workAreaTypeId) {
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

                // Determine scheduled status based on modality: virtual → Programado, presencial → Espera espacio
                String modalityNameSql = "SELECT LOWER(modalidad) FROM academico.tbmodalidades WHERE idmodalidad = :modalityId";
                String modalityName = getJdbcTemplate().queryForObject(
                                modalityNameSql, new MapSqlParameterSource("modalityId", modalityId), String.class);
                boolean isVirtual = modalityName != null && modalityName.contains("virtual");
                Integer scheduledPendienteId = isVirtual
                                ? getScheduledStatusId("Programado")
                                : getScheduledStatusId("Espera espacio");

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
                                "(idtiposesion, idmodalidad, idfranjahoraria, fechaprogramadarefuerzo, duracionestimado, "
                                +
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

                // Auto-register attendance rows only for participants who confirmed their participation
                String getParticipantsSql = "SELECT idparticipante FROM reforzamiento.tbparticipantes " +
                                "WHERE idsolicitudrefuerzo = :requestId AND participacion = TRUE";
                List<Integer> participantIds = getJdbcTemplate().queryForList(
                                getParticipantsSql,
                                new MapSqlParameterSource("requestId", requestId),
                                Integer.class);
                for (Integer participantId : participantIds) {
                        String insertAttendanceSql = "INSERT INTO reforzamiento.tbasistenciasrefuerzos " +
                                        "(idrefuerzoprogramado, idparticipante, asistencia) " +
                                        "VALUES (:scheduledId, :participantId, FALSE)";
                        MapSqlParameterSource attendanceParams = new MapSqlParameterSource();
                        attendanceParams.addValue("scheduledId", scheduledId);
                        attendanceParams.addValue("participantId", participantId);
                        getJdbcTemplate().update(insertAttendanceSql, attendanceParams);
                }

                // If in-person modality, insert on-site record with workAreaTypeId
                if (workAreaTypeId != null) {
                        String insertPresencialSql = "INSERT INTO reforzamiento.tbrefuerzospresenciales " +
                                        "(idrefuerzoprogramado, idtipoareatrabajo, estado) " +
                                        "VALUES (:scheduledId, :workAreaTypeId, TRUE)";
                        MapSqlParameterSource presencialParams = new MapSqlParameterSource();
                        presencialParams.addValue("scheduledId", scheduledId);
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
                        String reason, Integer workAreaTypeId) {
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

                // Update on-site reinforcement record if workAreaTypeId provided (Presencial)
                if (workAreaTypeId != null) {
                        String checkPresencialSql = "SELECT COUNT(*) FROM reforzamiento.tbrefuerzospresenciales " +
                                        "WHERE idrefuerzoprogramado = :scheduledId";
                        List<Integer> presencialRows = getJdbcTemplate().queryForList(checkPresencialSql,
                                        new MapSqlParameterSource("scheduledId", scheduledId), Integer.class);
                        Integer presencialCount = presencialRows.isEmpty() ? 0 : presencialRows.get(0);

                        if (presencialCount != null && presencialCount > 0) {
                                String updatePresencialSql = "UPDATE reforzamiento.tbrefuerzospresenciales " +
                                                "SET idtipoareatrabajo = :workAreaTypeId " +
                                                "WHERE idrefuerzoprogramado = :scheduledId";
                                MapSqlParameterSource presencialParams = new MapSqlParameterSource();
                                presencialParams.addValue("workAreaTypeId", workAreaTypeId);
                                presencialParams.addValue("scheduledId", scheduledId);
                                getJdbcTemplate().update(updatePresencialSql, presencialParams);
                        } else {
                                String insertPresencialSql = "INSERT INTO reforzamiento.tbrefuerzospresenciales " +
                                                "(idrefuerzoprogramado, idtipoareatrabajo, estado) " +
                                                "VALUES (:scheduledId, :workAreaTypeId, TRUE)";
                                MapSqlParameterSource presencialParams = new MapSqlParameterSource();
                                presencialParams.addValue("scheduledId", scheduledId);
                                presencialParams.addValue("workAreaTypeId", workAreaTypeId);
                                getJdbcTemplate().update(insertPresencialSql, presencialParams);
                        }
                }

                return new TeacherActionResponseDTO(scheduledId, "RESCHEDULED",
                                "Sesión reprogramada correctamente");
        }

        @Override
        @Transactional
        public TeacherActionResponseDTO cancelSession(Integer userId, Integer requestId, String reason) {
                Integer teacherId = getTeacherId(userId);

                // Verify the teacher owns this request
                String checkRequestSql = "SELECT COUNT(*) FROM reforzamiento.tbsolicitudesrefuerzos " +
                                "WHERE idsolicitudrefuerzo = :requestId AND iddocente = :teacherId";
                MapSqlParameterSource checkParams = new MapSqlParameterSource();
                checkParams.addValue("requestId", requestId);
                checkParams.addValue("teacherId", teacherId);
                List<Integer> checkRows = getJdbcTemplate().queryForList(checkRequestSql, checkParams, Integer.class);
                if (checkRows.isEmpty() || checkRows.get(0) == 0) {
                        return new TeacherActionResponseDTO(requestId, "ERROR",
                                        "Solicitud no encontrada o no pertenece a este docente");
                }

                // Resolve scheduledId from requestId via the detail table
                String findScheduledSql = "SELECT d.idrefuerzoprogramado " +
                                "FROM reforzamiento.tbdetallesrefuerzosprogramadas d " +
                                "WHERE d.idsolicitudrefuerzo = :requestId AND d.estado = TRUE LIMIT 1";
                List<Integer> scheduledRows = getJdbcTemplate().queryForList(findScheduledSql,
                                new MapSqlParameterSource("requestId", requestId), Integer.class);
                if (scheduledRows.isEmpty()) {
                        return new TeacherActionResponseDTO(requestId, "ERROR",
                                        "No se encontró sesión programada activa para esta solicitud");
                }
                Integer scheduledId = scheduledRows.get(0);

                // Get cancelled status ID for scheduled reinforcement
                Integer canceladaScheduledId = getScheduledStatusId("Cancelado");

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
