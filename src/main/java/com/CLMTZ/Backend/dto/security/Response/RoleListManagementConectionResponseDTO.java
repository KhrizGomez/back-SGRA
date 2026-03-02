package com.CLMTZ.Backend.dto.security.Response;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleListManagementConectionResponseDTO {
    private Integer pidgrol;
    private String pgrol;
    private String pgdescripcion;
    private Boolean prelacion;
}
