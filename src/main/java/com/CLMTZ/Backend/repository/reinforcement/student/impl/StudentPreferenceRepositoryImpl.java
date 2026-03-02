package com.CLMTZ.Backend.repository.reinforcement.student.impl;

import com.CLMTZ.Backend.config.DynamicDataSourceService;
import com.CLMTZ.Backend.dto.reinforcement.student.NotificationChannelDTO;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentPreferenceDTO;
import com.CLMTZ.Backend.repository.reinforcement.student.StudentPreferenceRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class StudentPreferenceRepositoryImpl implements StudentPreferenceRepository {

    private final DynamicDataSourceService dynamicDataSourceService;

    public StudentPreferenceRepositoryImpl(DynamicDataSourceService dynamicDataSourceService) {
        this.dynamicDataSourceService = dynamicDataSourceService;
    }

    private NamedParameterJdbcTemplate getJdbcTemplate() {
        return dynamicDataSourceService.getJdbcTemplate();
    }

    @Override
    public List<NotificationChannelDTO> listActiveChannels() {
        String sql = "SELECT idcanalnotificacion, nombrecanal FROM general.vw_canales_activos ORDER BY nombrecanal";

        return getJdbcTemplate().query(sql, (rs, rowNum) -> {
            NotificationChannelDTO dto = new NotificationChannelDTO();
            dto.setChannelId(rs.getInt("idcanalnotificacion"));
            dto.setChannelName(rs.getString("nombrecanal"));
            return dto;
        });
    }

    @Override
    public StudentPreferenceDTO getPreferenceByUser(Integer userId) {
        String sql = "SELECT idpreferencia, idusuario, idcanalnotificacion, nombrecanal, anticipacionrecordatorio " +
                     "FROM general.vw_preferencia_usuario WHERE idusuario = :userId";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);

        List<StudentPreferenceDTO> results = getJdbcTemplate().query(sql, params, (rs, rowNum) -> {
            StudentPreferenceDTO dto = new StudentPreferenceDTO();
            dto.setPreferenceId(rs.getInt("idpreferencia"));
            dto.setUserId(rs.getInt("idusuario"));
            dto.setChannelId(rs.getInt("idcanalnotificacion"));
            dto.setChannelName(rs.getString("nombrecanal"));
            dto.setReminderAnticipation(rs.getInt("anticipacionrecordatorio"));
            return dto;
        });

        if (results.isEmpty()) {
            return null;
        }

        return results.get(0);
    }

    @Override
    public void upsertPreference(Integer userId, Integer channelId, Integer reminderAnticipation) {
        String sql = "CALL general.sp_up_guardar_preferencia_unica(:userId, :channelId, :reminderAnticipation)";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("channelId", channelId);
        params.addValue("reminderAnticipation", reminderAnticipation);

        getJdbcTemplate().update(sql, params);
    }
}