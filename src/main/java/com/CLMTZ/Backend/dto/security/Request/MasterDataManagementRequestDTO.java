package com.CLMTZ.Backend.dto.security.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MasterDataManagementRequestDTO {
    private String esquematabla;
    private Integer id;
    private String nombre;
    private Boolean estado;
}
