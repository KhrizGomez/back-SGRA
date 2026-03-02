package com.CLMTZ.Backend.dto.security.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserListManagementResponseDTO {
    private Integer idgu;
    private String usuariogu;
    private Long rolesasignadosgu;
    private String estadogu;
    private LocalDate fechacreaciongu;
}

