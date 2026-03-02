package com.CLMTZ.Backend.dto.security.Request;

import java.util.List;

import com.CLMTZ.Backend.dto.security.Response.ModuleListManagementResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateRolePermissionsRequestDTO {
    private Integer roleId;
    private List<ModuleListManagementResponseDTO> permissions;
}
