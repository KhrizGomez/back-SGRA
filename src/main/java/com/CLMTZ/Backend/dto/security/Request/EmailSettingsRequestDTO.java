package com.CLMTZ.Backend.dto.security.Request;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmailSettingsRequestDTO {
    private Integer pidconfiguracioncorreo;
    private Integer idusuario;
    private String pcorreoemisor;
    private String paplicacionsontrasena;
    private LocalDateTime pfechahoracreacion;
    private String pestadop;
}
