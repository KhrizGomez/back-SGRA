package com.CLMTZ.Backend.dto.security.Response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleListResponseDTO {
    private Integer pidrol;
    private String prol;
    private List<RoleListManagementConectionResponseDTO> grolesconection;
}
