package com.CLMTZ.Backend.dto.reinforcement;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListOfWorkAreaRequestsRequestDTO {
    private Integer pidrefuerzopresencial;
    private Integer pidrefuerzoprogramado;
    private Integer pidtipoareatrabajo;
    private LocalTime phorainicio;
    private LocalTime phorariofin;
    private LocalDate pfechaprogramadarefuerzo;
    private String ptiposesion;
    private String pdocente;
    private Long pparticipantesesperados;
    private Long participantesconfirmados;
}
