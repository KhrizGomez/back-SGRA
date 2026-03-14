package com.CLMTZ.Backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BackupScheduleDTO {
    private boolean habilitado;
    private String expresionCron;
    private String fechaUltimaEjecucion;
    private String resultadoUltimaEjecucion;
}
