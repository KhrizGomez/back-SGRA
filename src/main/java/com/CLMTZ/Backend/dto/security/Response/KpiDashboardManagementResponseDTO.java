package com.CLMTZ.Backend.dto.security.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KpiDashboardManagementResponseDTO {
    private Long userActive;
    private Long userIncative;
    private Long rolesActive;
    private Long rolesInactive;
}
