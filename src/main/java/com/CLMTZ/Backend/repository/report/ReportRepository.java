package com.CLMTZ.Backend.repository.report;

import com.CLMTZ.Backend.dto.reinforcement.coordination.*;
import com.CLMTZ.Backend.dto.report.AttendanceDetailRowDTO;
import com.CLMTZ.Backend.dto.report.RequestDetailRowDTO;

import java.util.List;
import java.util.Map;

public interface ReportRepository {

    Integer getActivePeriodId();

    String getActivePeriodName();

    CoordinationDashboardKpisDTO getKpis(Integer periodId);

    CoordinationDashboardAsistenciaDTO getAsistencia(Integer periodId);

    List<CoordinationDashboardSolicitudesMateriaDTO> getSolicitudesPorMateria(Integer periodId);

    List<CoordinationDashboardModalidadDTO> getModalidades(Integer periodId);

    List<AttendanceDetailRowDTO> getAttendanceDetails(Integer periodId);

    List<RequestDetailRowDTO> getRequestDetails(Integer periodId);

    // ── Nuevos métodos para los tipos de reporte del frontend ──────────────
    List<Map<String, Object>> getPreviewBySubject(Integer periodId, String dateFrom, String dateTo);

    List<Map<String, Object>> getPreviewByTeacher(Integer periodId, String dateFrom, String dateTo);

    List<Map<String, Object>> getPreviewByParallel(Integer periodId, String dateFrom, String dateTo);

    List<Map<String, Object>> getPreviewByGrade(Integer periodId, String dateFrom, String dateTo);

    List<Map<String, Object>> getPreviewByStudent(Integer periodId, String dateFrom, String dateTo);
}
