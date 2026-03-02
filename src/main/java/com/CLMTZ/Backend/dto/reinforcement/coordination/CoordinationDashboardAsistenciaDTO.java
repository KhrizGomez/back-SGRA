package com.CLMTZ.Backend.dto.reinforcement.coordination;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CoordinationDashboardAsistenciaDTO {
    private Long totalSesionesRegistradas;
    private Long totalAsistencias;
    private Long totalInasistencias;
    private BigDecimal porcentajeAsistencia;
    private BigDecimal tasaInasistencia;
}
