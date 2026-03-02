package com.CLMTZ.Backend.dto.reinforcement.coordination;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CoordinationDashboardDTO {
    private CoordinationDashboardKpisDTO kpis;
    private CoordinationDashboardAsistenciaDTO asistencia;
    private List<CoordinationDashboardSolicitudesMateriaDTO> solicitudesPorMateria;
    private List<CoordinationDashboardModalidadDTO> modalidades;
}
