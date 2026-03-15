package com.CLMTZ.Backend.dto.report;

import com.CLMTZ.Backend.dto.reinforcement.coordination.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportDataDTO {
    private String reportTitle;
    private String periodName;
    private String institutionName;
    private String institutionLogoUrl;
    private CoordinationDashboardKpisDTO kpis;
    private CoordinationDashboardAsistenciaDTO asistencia;
    private List<CoordinationDashboardSolicitudesMateriaDTO> solicitudesPorMateria;
    private List<CoordinationDashboardModalidadDTO> modalidades;
    private List<AttendanceDetailRowDTO> attendanceDetails;
    private List<RequestDetailRowDTO> requestDetails;
}
