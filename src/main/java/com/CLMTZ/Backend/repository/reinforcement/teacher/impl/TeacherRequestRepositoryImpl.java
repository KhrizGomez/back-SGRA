package com.CLMTZ.Backend.repository.reinforcement.teacher.impl;

import com.CLMTZ.Backend.config.DynamicDataSourceService;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentRequestSummaryDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherActionResponseDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherRequestItemDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherRequestsPageDTO;
import com.CLMTZ.Backend.repository.reinforcement.teacher.TeacherRequestRepository;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
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

            @Override
            public StudentRequestSummaryDTO getRequestSummary(Integer requestId) {
                        String sql = "SELECT * FROM reforzamiento.fn_sl_resumen_solicitud_notif(:requestId)";

                        MapSqlParameterSource params = new MapSqlParameterSource("requestId", requestId);
                        return getJdbcTemplate().query(sql, params, rs -> rs.next()
                                ? new StudentRequestSummaryDTO(
                                rs.getInt("request_id"),
                                rs.getString("student_name"),
                                rs.getString("student_email"),
                                rs.getString("teacher_name"),
                                rs.getString("teacher_email"),
                                rs.getString("subject_name"),
                                rs.getString("course_name"),
                                rs.getString("parallel_name"),
                                rs.getString("reason"))
                                : null);
            }

        private Integer getTeacherId(Integer userId) {
                String sql = "SELECT iddocente FROM academico.tbdocentes WHERE idusuario = :userId AND estado = TRUE";
                MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
                List<Integer> rows = getJdbcTemplate().queryForList(sql, params, Integer.class);
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

        private boolean isFunctionResolutionError(BadSqlGrammarException ex) {
                Throwable cause = ex;
                while (cause != null) {
                        if (cause instanceof SQLException sqlEx) {
                                String sqlState = sqlEx.getSQLState();
                                if ("42883".equals(sqlState)) {
                                        return true;
                                }
                        }
                        cause = cause.getCause();
                }
                String msg = ex.getMessage();
                return msg != null && msg.toLowerCase().contains("does not exist");
        }

        private TeacherActionResponseDTO mapActionResponseWithFallbacks(
                        MapSqlParameterSource params,
                        String... sqlCandidates) {
                BadSqlGrammarException last = null;
                for (int i = 0; i < sqlCandidates.length; i++) {
                        try {
                                return mapActionResponse(params, sqlCandidates[i]);
                        } catch (BadSqlGrammarException ex) {
                                last = ex;
                                boolean hasMoreCandidates = i < sqlCandidates.length - 1;
                                if (!hasMoreCandidates || !isFunctionResolutionError(ex)) {
                                        throw ex;
                                }
                        }
                }
                throw last;
        }

        private int readStatusId(ResultSet rs) throws SQLException {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                        String label = meta.getColumnLabel(i);
                        if ("status_id".equalsIgnoreCase(label)) {
                                return rs.getInt("status_id");
                        }
                }
                return rs.getInt("estado_id");
        }

        @Override
        public TeacherRequestsPageDTO getIncomingRequests(Integer userId, Integer statusId, Integer page,
                        Integer size) {
                MapSqlParameterSource params = new MapSqlParameterSource();
                params.addValue("userId", userId);
                params.addValue("statusId", statusId);
                params.addValue("page", page);
                params.addValue("size", size);
                Long total = getJdbcTemplate().queryForObject(
                                "SELECT reforzamiento.fn_sl_teacher_incoming_requests_count(:userId, :statusId)",
                                params,
                                Long.class);
                List<TeacherRequestItemDTO> items = new ArrayList<>();

                getJdbcTemplate().query(
                                "SELECT * FROM reforzamiento.fn_sl_teacher_incoming_requests_page(:userId, :statusId, :page, :size)",
                                params,
                                (rs) -> {
                        TeacherRequestItemDTO item = new TeacherRequestItemDTO();
                        item.setRequestId(rs.getInt("request_id"));
                        item.setStudentName(rs.getString("student_name"));
                        item.setSubjectName(rs.getString("subject_name"));
                        item.setSessionType(rs.getString("session_type"));
                        item.setReason(rs.getString("reason"));
                        item.setStatusName(rs.getString("status_name"));
                        item.setStatusId(readStatusId(rs));
                        Timestamp ts = rs.getTimestamp("created_at");
                        item.setCreatedAt(ts != null ? ts.toInstant().toString() : null);
                        int sessionTypeId = rs.getInt("session_type_id");
                        item.setIsGroupal(sessionTypeId == 2); // 2 = grupal typically
                        item.setParticipantCount(rs.getInt("participant_count"));
                        items.add(item);
                });

                return new TeacherRequestsPageDTO(items, total != null ? total : 0L, page, size);
        }

        @Override
        @Transactional
        public TeacherActionResponseDTO acceptRequest(Integer userId, Integer requestId, String scheduledDate,
                        Integer timeSlotId, Integer modalityId, String estimatedDuration,
                        String reason, Integer workAreaTypeId) {
                MapSqlParameterSource params = new MapSqlParameterSource();
                params.addValue("userId", userId);
                params.addValue("requestId", requestId);
                params.addValue("scheduledDate", scheduledDate);
                params.addValue("timeSlotId", timeSlotId);
                params.addValue("modalityId", modalityId);
                params.addValue("estimatedDuration", estimatedDuration);
                params.addValue("reason", reason);
                params.addValue("workAreaTypeId", workAreaTypeId);

                return mapActionResponseWithFallbacks(
                                params,
                                "SELECT * FROM reforzamiento.fn_tx_teacher_accept_request(" +
                                                ":userId::int, :requestId::int, :scheduledDate::text, :timeSlotId::int, :modalityId::int, " +
                                                ":estimatedDuration::text, :reason::text, :workAreaTypeId::int)",
                                "SELECT * FROM reforzamiento.fn_tx_teacher_accept_request(" +
                                                ":userId::int, :requestId::int, :scheduledDate::date, :timeSlotId::int, :modalityId::int, " +
                                                ":estimatedDuration::time, :reason::text, :workAreaTypeId::int)",
                                "SELECT * FROM reforzamiento.fn_tx_teacher_accept_request(" +
                                                ":userId::int, :requestId::int, :scheduledDate::text, :timeSlotId::int, :modalityId::int, " +
                                                ":estimatedDuration::text, :reason::text)",
                                "SELECT * FROM reforzamiento.fn_tx_teacher_accept_request(" +
                                                ":userId::int, :requestId::int, :scheduledDate::date, :timeSlotId::int, :modalityId::int, " +
                                                ":estimatedDuration::time, :reason::text)");
        }

        @Override
        @Transactional
        public TeacherActionResponseDTO rejectRequest(Integer userId, Integer requestId, String reason) {
                MapSqlParameterSource params = new MapSqlParameterSource();
                params.addValue("userId", userId);
                params.addValue("requestId", requestId);
                params.addValue("reason", reason);

                return mapActionResponse(
                                params,
                                "SELECT * FROM reforzamiento.fn_tx_teacher_reject_request(:userId::int, :requestId::int, :reason::text)");
        }

        @Override
        @Transactional
        public TeacherActionResponseDTO rescheduleRequest(Integer userId, Integer requestId, String scheduledDate,
                        Integer timeSlotId, Integer modalityId, String estimatedDuration,
                        String reason, Integer workAreaTypeId) {
                MapSqlParameterSource params = new MapSqlParameterSource();
                params.addValue("userId", userId);
                params.addValue("requestId", requestId);
                params.addValue("scheduledDate", scheduledDate);
                params.addValue("timeSlotId", timeSlotId);
                params.addValue("modalityId", modalityId);
                params.addValue("estimatedDuration", estimatedDuration);
                params.addValue("reason", reason);
                params.addValue("workAreaTypeId", workAreaTypeId);

                return mapActionResponseWithFallbacks(
                                params,
                                "SELECT * FROM reforzamiento.fn_tx_teacher_reschedule_request(" +
                                                ":userId::int, :requestId::int, :scheduledDate::text, :timeSlotId::int, :modalityId::int, " +
                                                ":estimatedDuration::text, :reason::text, :workAreaTypeId::int)",
                                "SELECT * FROM reforzamiento.fn_tx_teacher_reschedule_request(" +
                                                ":userId::int, :requestId::int, :scheduledDate::date, :timeSlotId::int, :modalityId::int, " +
                                                ":estimatedDuration::time, :reason::text, :workAreaTypeId::int)",
                                "SELECT * FROM reforzamiento.fn_tx_teacher_reschedule_request(" +
                                                ":userId::int, :requestId::int, :scheduledDate::text, :timeSlotId::int, :modalityId::int, " +
                                                ":estimatedDuration::text, :reason::text)",
                                "SELECT * FROM reforzamiento.fn_tx_teacher_reschedule_request(" +
                                                ":userId::int, :requestId::int, :scheduledDate::date, :timeSlotId::int, :modalityId::int, " +
                                                ":estimatedDuration::time, :reason::text)");
        }

        @Override
        @Transactional
        public TeacherActionResponseDTO cancelSession(Integer userId, Integer requestId, String reason) {
                MapSqlParameterSource params = new MapSqlParameterSource();
                params.addValue("userId", userId);
                params.addValue("requestId", requestId);
                params.addValue("reason", reason);

                return mapActionResponse(
                                params,
                                "SELECT * FROM reforzamiento.fn_tx_teacher_cancel_session(:userId::int, :requestId::int, :reason::text)");
        }
}
