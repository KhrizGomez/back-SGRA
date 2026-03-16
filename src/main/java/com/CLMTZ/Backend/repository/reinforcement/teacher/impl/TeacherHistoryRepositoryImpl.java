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
     MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("page", page);
        params.addValue("size", size);

        Long total = getJdbcTemplate().queryForObject(
                "SELECT reforzamiento.fn_sl_teacher_session_history_count(:userId)",
                params,
                Long.class);
        List<TeacherSessionHistoryItemDTO> items = new ArrayList<>();

        getJdbcTemplate().query(
                "SELECT * FROM reforzamiento.fn_sl_teacher_session_history_page(:userId, :page, :size)",
                params,
                (rs) -> {
            TeacherSessionHistoryItemDTO item = new TeacherSessionHistoryItemDTO();
            item.setScheduledId(rs.getInt("scheduled_id"));
            item.setSubjectName(rs.getString("subject_name"));

            Date date = rs.getDate("scheduled_date");
            item.setScheduledDate(date != null ? date.toString() : null);

            item.setModality(rs.getString("modalidad"));

            Time duration = rs.getTime("estimated_duration");
            item.setEstimatedDuration(duration != null ? duration.toString() : null);

            item.setTimeSlot(rs.getString("time_slot"));
            item.setStatusName(rs.getString("status_name"));
            item.setSessionType(rs.getString("session_type"));
            item.setStudentCount(rs.getInt("request_count"));
            int total_ = rs.getInt("total_participants");
            int attended_ = rs.getInt("attended_count");
            item.setTotalParticipants(total_);
            item.setAttendedCount(attended_);
            item.setAttendancePercentage(total_ > 0 ? Math.round((attended_ * 100.0 / total_) * 10.0) / 10.0 : 0.0);
            item.setResourceCount(rs.getInt("resource_count"));
            items.add(item);
        });

        return new TeacherSessionHistoryPageDTO(items, total != null ? total : 0L, page, size);
    }

    @Override
    public TeacherSessionHistoryDetailDTO getSessionHistoryDetail(Integer userId, Integer scheduledId) {
        List<TeacherSessionHistoryDetailDTO> baseResult = new ArrayList<>();
        MapSqlParameterSource baseParams = new MapSqlParameterSource();
        baseParams.addValue("scheduledId", scheduledId);
        baseParams.addValue("userId", userId);
        getJdbcTemplate().query(
                "SELECT * FROM reforzamiento.fn_sl_teacher_session_history_detail_base(:userId, :scheduledId)",
                baseParams,
                rs -> {
            TeacherSessionHistoryDetailDTO dto = new TeacherSessionHistoryDetailDTO();
            dto.setScheduledId(rs.getInt("scheduled_id"));
            dto.setSubjectName(rs.getString("subject_name"));
            Date d = rs.getDate("scheduled_date");
            dto.setScheduledDate(d != null ? d.toString() : null);
            dto.setModality(rs.getString("modalidad"));
            dto.setTimeSlot(rs.getString("time_slot"));
            dto.setSessionType(rs.getString("session_type"));
            dto.setStatusName(rs.getString("status_name"));
            Time dur = rs.getTime("estimated_duration");
            dto.setEstimatedDuration(dur != null ? dur.toString().substring(0, 5) : null);
            baseResult.add(dto);
        });

        if (baseResult.isEmpty()) {
            throw new RuntimeException("Sesi\u00f3n no encontrada o no pertenece a este docente");
        }

        TeacherSessionHistoryDetailDTO detail = baseResult.get(0);

        // 2. Performed info (observation + actual duration)
        getJdbcTemplate().query(
                "SELECT * FROM reforzamiento.fn_sl_teacher_session_history_detail_performed(:scheduledId)",
                new MapSqlParameterSource("scheduledId", scheduledId),
                rs -> {
            detail.setObservation(rs.getString("observacion"));
            Time actualDur = rs.getTime("duracion");
            detail.setActualDuration(actualDur != null ? actualDur.toString().substring(0, 5) : null);
        });

        // 3. Attendance per student
        List<AttendanceStudentDTO> attendanceList = new ArrayList<>();
        getJdbcTemplate().query(
                "SELECT * FROM reforzamiento.fn_sl_teacher_session_history_detail_attendance(:scheduledId)",
                new MapSqlParameterSource("scheduledId", scheduledId),
                rs -> {
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
        List<String> resources = getJdbcTemplate().queryForList(
                "SELECT * FROM reforzamiento.fn_sl_teacher_session_history_detail_resources(:scheduledId)",
                new MapSqlParameterSource("scheduledId", scheduledId),
                String.class);
        detail.setResources(resources);

        // 5. Virtual link
        List<String> vl = getJdbcTemplate().queryForList(
                "SELECT * FROM reforzamiento.fn_sl_teacher_session_history_detail_virtual_links(:scheduledId)",
                new MapSqlParameterSource("scheduledId", scheduledId),
                String.class);
        if (!vl.isEmpty()) {
            detail.setVirtualLink(vl.get(0));
        }

        return detail;
    }
}
