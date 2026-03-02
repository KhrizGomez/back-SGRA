package com.CLMTZ.Backend.dto.security.Request;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MasterManagementRequestDTO {
    private String esquematabla;
    private Integer id;
    private String nombre;
    private Boolean estado;
}
