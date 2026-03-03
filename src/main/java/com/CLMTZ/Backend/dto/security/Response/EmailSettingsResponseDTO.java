package com.CLMTZ.Backend.dto.security.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailSettingsResponseDTO {
    private String servidorSmtp;
    private Integer puertoSmtp;
    private Boolean usaSSL;
    private String correoEmisor;
    private String contrasenaAplicacion;
    private String nombreRemitente;
}
