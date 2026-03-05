package com.CLMTZ.Backend.dto.academic;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PeriodDTO {
    private Integer idperiodo;
    private String periodo;
    private LocalDate fechainicio;
    private LocalDate fechafin;
    private Boolean estado;
}
