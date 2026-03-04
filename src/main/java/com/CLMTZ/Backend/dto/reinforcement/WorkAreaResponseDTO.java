package com.CLMTZ.Backend.dto.reinforcement;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkAreaResponseDTO {
    private Integer pidareatrabajo;
    private String pnumeroarea;
    private String pdisponibilidad;
    private Integer pcapacidad;
    private Short pplanta;
    private String pareatrabajo;
}
