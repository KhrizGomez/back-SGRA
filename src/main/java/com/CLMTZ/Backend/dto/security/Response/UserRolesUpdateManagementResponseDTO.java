package com.CLMTZ.Backend.dto.security.Response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserRolesUpdateManagementResponseDTO {
    private Integer userGId;
    private String user;
    private String password;
    private String state;
    private List<Integer> roles;
    @JsonIgnore 
    private String rolesasignadosgu;
}
