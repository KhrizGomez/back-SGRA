package com.CLMTZ.Backend.dto.security.Response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserRoleManagementResponseDTO {
    private Integer idgu;
    private String usuariogu;
    private String contrasena;
    private String estadogu;
    private List<Integer> roles;
    @JsonIgnore 
    private String rolesasignadosgu;
}
