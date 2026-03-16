package com.CLMTZ.Backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BackupScheduleEntryDTO {
    private Integer id;
    private Integer idusuario;
    private boolean habilitado;
    private String  frecuencia;
    private String  diaSemana;
    private String  diaMes;
    private String  meses;
    private int     hora;
    private int     minuto;
    private String  fechaUltimaEjecucion;
    private String  resultadoUltimaEjecucion;
}
