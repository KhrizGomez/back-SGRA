package com.CLMTZ.Backend.repository.report;

import com.CLMTZ.Backend.dto.reinforcement.coordination.*;
import com.CLMTZ.Backend.dto.report.AttendanceDetailRowDTO;
import com.CLMTZ.Backend.dto.report.RequestDetailRowDTO;

import java.util.List;

public interface ReportRepository {

    Integer getActivePeriodId();

    String getActivePeriodName();

    CoordinationDashboardKpisDTO getKpis(Integer periodId);

    CoordinationDashboardAsistenciaDTO getAsistencia(Integer periodId);

    List<CoordinationDashboardSolicitudesMateriaDTO> getSolicitudesPorMateria(Integer periodId);

    List<CoordinationDashboardModalidadDTO> getModalidades(Integer periodId);

    List<AttendanceDetailRowDTO> getAttendanceDetails(Integer periodId);

    List<RequestDetailRowDTO> getRequestDetails(Integer periodId);
}
