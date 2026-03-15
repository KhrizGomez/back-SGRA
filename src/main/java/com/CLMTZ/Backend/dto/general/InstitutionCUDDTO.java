package com.CLMTZ.Backend.dto.general;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InstitutionCUDDTO {
    private Integer lidinstitucion;
    private Boolean lestado;
    private String lurllogo;
}
