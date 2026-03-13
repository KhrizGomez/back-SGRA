package com.CLMTZ.Backend.dto.security.Response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccessAuditResponseDTO {
    private Integer aidauditoriaacceso;
    private String ausuario;
    private String adireccionip;
    private String anavegador;
    private String aso;
    private String asesion;
    private LocalDateTime afechaacceso;
    private String aaccion;
}
