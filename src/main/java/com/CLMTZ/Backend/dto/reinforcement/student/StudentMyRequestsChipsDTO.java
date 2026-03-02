package com.CLMTZ.Backend.dto.reinforcement.student;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentMyRequestsChipsDTO {
    private Long pending;
    private Long accepted;
    private Long scheduled;
    private Long completed;
}