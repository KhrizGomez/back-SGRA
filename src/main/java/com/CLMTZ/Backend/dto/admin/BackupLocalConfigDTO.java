package com.CLMTZ.Backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BackupLocalConfigDTO {
    private String  ruta;
    private Integer idusuario;
    private String  fechaConfiguracion;
}
