package com.CLMTZ.Backend.dto.reinforcement.coordination;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CoordinationDashboardKpisDTO {
    private Long totalSolicitudes;
    private Long pendientes;
    private Long gestionadas;
}
