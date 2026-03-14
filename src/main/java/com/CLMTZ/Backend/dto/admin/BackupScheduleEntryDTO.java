package com.CLMTZ.Backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BackupScheduleEntryDTO {
    private Integer id;
    private boolean habilitado;
    private String  frecuencia;               // DIARIO | SEMANAL | MENSUAL
    private String  diaSemana;                // SUN, MON, ... (solo SEMANAL)
    private Integer diaMes;                   // 1-31   (solo MENSUAL)
    private int     hora;
    private int     minuto;
    private String  fechaUltimaEjecucion;
    private String  resultadoUltimaEjecucion;
}
