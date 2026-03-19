package com.CLMTZ.Backend.repository.reinforcement.teacher.impl;

import com.CLMTZ.Backend.config.DynamicDataSourceService;
import com.CLMTZ.Backend.dto.reinforcement.TeacherAvailabilityBatchDTO;
import com.CLMTZ.Backend.dto.reinforcement.TeacherAvailabilitySlotDTO;
import com.CLMTZ.Backend.dto.reinforcement.teacher.TeacherActionResponseDTO;
import com.CLMTZ.Backend.repository.reinforcement.teacher.TeacherAvailabilityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Repository
public class TeacherAvailabilityRepositoryImpl implements TeacherAvailabilityRepository {

    private static final String[] DAY_NAMES = {
        "", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"
    };
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final DynamicDataSourceService dynamicDataSourceService;
    private final ObjectMapper objectMapper;

    public TeacherAvailabilityRepositoryImpl(DynamicDataSourceService dynamicDataSourceService,
                                             ObjectMapper objectMapper) {
        this.dynamicDataSourceService = dynamicDataSourceService;
        this.objectMapper = objectMapper;
    }

    private NamedParameterJdbcTemplate jdbc() {
        return dynamicDataSourceService.getJdbcTemplate();
    }

    @Override
    public List<TeacherAvailabilitySlotDTO> getAvailability(Integer userId, Integer periodId) {
        String sql = "SELECT * FROM reforzamiento.fn_sl_teacher_availability(:userId, :periodId)";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("periodId", periodId);

        List<TeacherAvailabilitySlotDTO> result = new ArrayList<>();
        jdbc().query(sql, params, rs -> {
            short day = rs.getShort("day_of_week");
            LocalTime start = rs.getObject("start_time", LocalTime.class);
            LocalTime end   = rs.getObject("end_time",   LocalTime.class);
            result.add(new TeacherAvailabilitySlotDTO(
                    rs.getInt("availability_id"),
                    day,
                    dayName(day),
                    rs.getInt("time_slot_id"),
                    start != null ? start.format(TIME_FMT) : null,
                    end   != null ? end.format(TIME_FMT)   : null
            ));
        });
        return result;
    }

    @Override
    public TeacherActionResponseDTO saveAvailability(Integer userId, Integer periodId,
                                                     List<TeacherAvailabilityBatchDTO.SlotDTO> slots) {
        try {
            String slotsJson = objectMapper.writeValueAsString(slots);
            String sql = "SELECT * FROM reforzamiento.fn_tx_teacher_save_availability(:userId::int, :periodId::int, :slots::jsonb)";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("periodId", periodId)
                    .addValue("slots", slotsJson);

            List<TeacherActionResponseDTO> rows = jdbc().query(sql, params, (rs, rn) ->
                    new TeacherActionResponseDTO(rs.getInt("entity_id"),
                            rs.getString("status"),
                            rs.getString("message")));

            if (rows.isEmpty()) {
                throw new RuntimeException("No se obtuvo respuesta de la base de datos");
            }
            return rows.get(0);
        } catch (Exception e) {
            throw new RuntimeException("Error al guardar disponibilidad: " + e.getMessage(), e);
        }
    }

    @Override
    public List<TeacherAvailabilitySlotDTO> getAvailabilityForStudent(Integer teacherId, Integer periodId) {
        String sql = "SELECT * FROM reforzamiento.fn_sl_teacher_availability_for_student(:teacherId, :periodId)";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("teacherId", teacherId)
                .addValue("periodId", periodId);

        List<TeacherAvailabilitySlotDTO> result = new ArrayList<>();
        jdbc().query(sql, params, rs -> {
            short day = rs.getShort("day_of_week");
            LocalTime start = rs.getObject("start_time", LocalTime.class);
            LocalTime end   = rs.getObject("end_time",   LocalTime.class);
            result.add(new TeacherAvailabilitySlotDTO(
                    null,
                    day,
                    dayName(day),
                    rs.getInt("time_slot_id"),
                    start != null ? start.format(TIME_FMT) : null,
                    end   != null ? end.format(TIME_FMT)   : null
            ));
        });
        return result;
    }

    private String dayName(short day) {
        if (day < 1 || day > 7) return "Desconocido";
        return DAY_NAMES[day];
    }
}