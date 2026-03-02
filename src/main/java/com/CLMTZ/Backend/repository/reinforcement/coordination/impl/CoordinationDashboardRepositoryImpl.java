package com.CLMTZ.Backend.repository.reinforcement.coordination.impl;

import com.CLMTZ.Backend.config.DynamicDataSourceService;
import com.CLMTZ.Backend.dto.reinforcement.coordination.*;
import com.CLMTZ.Backend.repository.reinforcement.coordination.CoordinationDashboardRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class CoordinationDashboardRepositoryImpl implements CoordinationDashboardRepository {

    private final DynamicDataSourceService dynamicDataSourceService;

    public CoordinationDashboardRepositoryImpl(DynamicDataSourceService dynamicDataSourceService) {
        this.dynamicDataSourceService = dynamicDataSourceService;
    }

    private NamedParameterJdbcTemplate getJdbcTemplate() {
        return dynamicDataSourceService.getJdbcTemplate();
    }

    private Integer getActivePeriodId() {
        String sql = "SELECT academico.fn_sl_id_periodo_activo()";
        return getJdbcTemplate().getJdbcTemplate().queryForObject(sql, Integer.class);
    }

    @Override
    public CoordinationDashboardDTO getDashboard() {
        Integer periodId = getActivePeriodId();

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("periodoId", periodId);

        return new CoordinationDashboardDTO(
                getKpis(params),
                getAsistencia(params),
                getSolicitudesPorMateria(params),
                getModalidades(params)
        );
    }

    private CoordinationDashboardKpisDTO getKpis(MapSqlParameterSource params) {
        String sql = "SELECT total_solicitudes, pendientes, gestionadas " +
                     "FROM reforzamiento.vw_dashboard_kpis_solicitudes " +
                     "WHERE idperiodo = :periodoId";

        List<CoordinationDashboardKpisDTO> results = getJdbcTemplate().query(sql, params, (rs, rowNum) ->
                new CoordinationDashboardKpisDTO(
                        rs.getLong("total_solicitudes"),
                        rs.getLong("pendientes"),
                        rs.getLong("gestionadas")
                ));

        return results.isEmpty() ? new CoordinationDashboardKpisDTO(0L, 0L, 0L) : results.get(0);
    }

    private CoordinationDashboardAsistenciaDTO getAsistencia(MapSqlParameterSource params) {
        String sql = "SELECT total_sesiones_registradas, total_asistencias, total_inasistencias, " +
                     "porcentaje_asistencia, tasa_inasistencia " +
                     "FROM reforzamiento.vw_dashboard_asistencia " +
                     "WHERE idperiodo = :periodoId";

        List<CoordinationDashboardAsistenciaDTO> results = getJdbcTemplate().query(sql, params, (rs, rowNum) ->
                new CoordinationDashboardAsistenciaDTO(
                        rs.getLong("total_sesiones_registradas"),
                        rs.getLong("total_asistencias"),
                        rs.getLong("total_inasistencias"),
                        rs.getBigDecimal("porcentaje_asistencia"),
                        rs.getBigDecimal("tasa_inasistencia")
                ));

        return results.isEmpty()
                ? new CoordinationDashboardAsistenciaDTO(0L, 0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO)
                : results.get(0);
    }

    private List<CoordinationDashboardSolicitudesMateriaDTO> getSolicitudesPorMateria(MapSqlParameterSource params) {
        String sql = "SELECT asignatura, total_materia, pendientes, gestionadas " +
                     "FROM reforzamiento.vw_dashboard_solicitudes_materia " +
                     "WHERE idperiodo = :periodoId";

        return getJdbcTemplate().query(sql, params, (rs, rowNum) ->
                new CoordinationDashboardSolicitudesMateriaDTO(
                        rs.getString("asignatura"),
                        rs.getLong("total_materia"),
                        rs.getLong("pendientes"),
                        rs.getLong("gestionadas")
                ));
    }

    private List<CoordinationDashboardModalidadDTO> getModalidades(MapSqlParameterSource params) {
        String sql = "SELECT modalidad, total " +
                     "FROM reforzamiento.vw_dashboard_modalidad " +
                     "WHERE idperiodo = :periodoId";

        return getJdbcTemplate().query(sql, params, (rs, rowNum) ->
                new CoordinationDashboardModalidadDTO(
                        rs.getString("modalidad"),
                        rs.getLong("total")
                ));
    }
}
