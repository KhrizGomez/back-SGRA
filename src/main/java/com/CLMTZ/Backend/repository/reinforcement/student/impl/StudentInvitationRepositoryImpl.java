package com.CLMTZ.Backend.repository.reinforcement.student.impl;

import com.CLMTZ.Backend.config.DynamicDataSourceService;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentInvitationItemDTO;
import com.CLMTZ.Backend.repository.reinforcement.student.StudentInvitationRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class StudentInvitationRepositoryImpl implements StudentInvitationRepository {

    private final DynamicDataSourceService dynamicDataSourceService;

    public StudentInvitationRepositoryImpl(DynamicDataSourceService dynamicDataSourceService) {
        this.dynamicDataSourceService = dynamicDataSourceService;
    }

    private NamedParameterJdbcTemplate getJdbcTemplate() {
        return dynamicDataSourceService.getJdbcTemplate();
    }

    @Override
    public List<StudentInvitationItemDTO> listPendingInvitations(Integer userId) {
        String sql = "SELECT * FROM reforzamiento.fn_sl_invitaciones_grupales_estudiante(:userId)";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);

        return getJdbcTemplate().query(sql, params, (rs, rowNum) -> new StudentInvitationItemDTO(
                rs.getInt("idparticipante"),
                rs.getInt("idsolicitudrefuerzo"),
                rs.getString("asignatura"),
                rs.getShort("semestre"),
                rs.getString("solicitante"),
                rs.getString("correo_solicitante"),
                rs.getString("docente"),
                rs.getString("tipo_sesion"),
                rs.getString("motivo"),
                rs.getTimestamp("fecha_solicitud"),
                rs.getLong("total_invitados"),
                rs.getLong("total_aceptados")
        ));
    }

    @Override
    public Boolean respondInvitation(Integer userId, Integer participantId, Boolean accept) {
        String sql = "SELECT reforzamiento.fn_up_responder_invitacion_grupal(:userId, :participantId, :accept)";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("participantId", participantId);
        params.addValue("accept", accept);

        Boolean result = getJdbcTemplate().queryForObject(sql, params, Boolean.class);
        return result != null && result;
    }
}

