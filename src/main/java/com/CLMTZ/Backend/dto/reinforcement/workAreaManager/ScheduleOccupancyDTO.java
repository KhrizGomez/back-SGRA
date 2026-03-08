package com.CLMTZ.Backend.dto.reinforcement.workAreaManager;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleOccupancyDTO {
    private Integer pidocupacion;
    private Integer pidareatrabajo;
    private String pareatrabajo;
    private String pnumeroarea;
    private String pdiasemana;
    private LocalDate pfecha;
    private LocalTime phorainicio;
    private LocalTime phorafin;
    private String pdocente;
    private String pmateria;
    private String ptiposesion;
}
