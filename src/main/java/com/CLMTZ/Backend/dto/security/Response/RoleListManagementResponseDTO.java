package com.CLMTZ.Backend.dto.security.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleListManagementResponseDTO{
    private Integer idg;
    private String nombreg;
    private String descripciong;
    private String estadog;
    private Long permisosg;
    private LocalDate fechacreaciong;
}

