package com.CLMTZ.Backend.dto.security.Request;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RolesServersAssignmentRequestDTO {
    private Integer pidgrol;
    private String pgrol;
    private String pgdescripcion;
    private Boolean prelacion;
}
