package com.CLMTZ.Backend.dto.security.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MasterTableListManagementResponseDTO {
    private String pesquematabla;
    private String pnombre;
    private String pdescripcion;
}
