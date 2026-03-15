package com.CLMTZ.Backend.dto.security.Response;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataAuditResponseDTO {
    private String ausuario;
    private LocalDateTime afechaacceso;
    private LocalDateTime afechacierre;
    private String aaccion;
    private String atablaafectada;
    private Integer aidregistro;
    private LocalDateTime afechahoraaccion;
    private Map<String, Object> adatosnuevos;
    private Map<String, Object> adatosantiguos;
}
