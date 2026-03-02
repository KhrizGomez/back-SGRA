package com.CLMTZ.Backend.dto.reinforcement.student;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta al aceptar/rechazar una invitación grupal.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentInvitationResponseDTO {
    private Boolean success;
    private String message;
}

