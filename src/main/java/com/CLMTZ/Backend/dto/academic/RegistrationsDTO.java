package com.CLMTZ.Backend.dto.academic;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationsDTO {
    private Integer registrationId;
    private LocalDate date;
    private Boolean status;
    private Integer periodId;
    private Integer studentId;
}
