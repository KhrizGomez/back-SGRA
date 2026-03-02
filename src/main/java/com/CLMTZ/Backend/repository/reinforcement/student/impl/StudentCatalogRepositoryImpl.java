package com.CLMTZ.Backend.repository.reinforcement.student.impl;

import com.CLMTZ.Backend.config.DynamicDataSourceService;
import com.CLMTZ.Backend.dto.reinforcement.student.*;
import com.CLMTZ.Backend.repository.reinforcement.student.StudentCatalogRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class StudentCatalogRepositoryImpl implements StudentCatalogRepository {

    private final DynamicDataSourceService dynamicDataSourceService;

    public StudentCatalogRepositoryImpl(DynamicDataSourceService dynamicDataSourceService) {
        this.dynamicDataSourceService = dynamicDataSourceService;
    }

    private NamedParameterJdbcTemplate getJdbcTemplate() {
        return dynamicDataSourceService.getJdbcTemplate();
    }

    @Override
    public List<SubjectItemDTO> listEnrolledSubjects(Integer userId) {
        String sql = "SELECT * FROM reforzamiento.fn_sl_asignaturas_estudiante(:userId)";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);

        return getJdbcTemplate().query(sql, params, (rs, rowNum) -> new SubjectItemDTO(
                rs.getInt("idasignatura"),
                rs.getString("asignatura"),
                rs.getShort("semestre")
        ));
    }

    @Override
    public StudentSubjectTeacherDTO getTeacherForStudentSubject(Integer userId, Integer subjectId) {
        String sql = "SELECT * FROM reforzamiento.fn_sl_docente_por_asignatura_estudiante(:userId, :subjectId)";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("subjectId", subjectId);

        List<StudentSubjectTeacherDTO> results = getJdbcTemplate().query(sql, params, (rs, rowNum) ->
                new StudentSubjectTeacherDTO(
                        rs.getInt("iddocente"),
                        rs.getString("nombre_completo"),
                        rs.getString("correo")
                ));

        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public List<SessionTypeItemDTO> listSessionTypes() {
        String sql = "SELECT * FROM reforzamiento.fn_sl_cat_tipos_sesion()";
        return getJdbcTemplate().query(sql, (rs, rowNum) -> new SessionTypeItemDTO(
                rs.getInt("idtiposesion"),
                rs.getString("tiposesion")
        ));
    }

    @Override
    public ActivePeriodDTO getActivePeriod() {
        String sql = "SELECT * FROM reforzamiento.fn_sl_periodo_activo()";

        List<ActivePeriodDTO> results = getJdbcTemplate().query(sql, (rs, rowNum) ->
                new ActivePeriodDTO(
                        rs.getInt("idperiodo"),
                        rs.getString("periodo")
                ));

        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public List<ClassmateItemDTO> listClassmatesBySubject(Integer subjectId, Integer currentUserId) {
        String sql = "SELECT * FROM reforzamiento.fn_sl_companeros_por_asignatura(:subjectId, :currentUserId)";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("subjectId", subjectId);
        params.addValue("currentUserId", currentUserId);

        return getJdbcTemplate().query(sql, params, (rs, rowNum) -> new ClassmateItemDTO(
                rs.getInt("student_id"),
                rs.getString("full_name"),
                rs.getString("email")
        ));
    }

    @Override
    public void addResourceUrl(Integer requestId, String fileUrl) {
        String sql = "SELECT reforzamiento.fn_in_recurso_solicitud(:requestId, :fileUrl)";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("requestId", requestId);
        params.addValue("fileUrl", fileUrl);
        getJdbcTemplate().queryForObject(sql, params, Integer.class);
    }
}