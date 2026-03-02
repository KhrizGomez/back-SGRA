package com.CLMTZ.Backend.dto.security.Request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserManagementRequestDTO {
    private Integer userGId;
    private String user;
    private String password;
    private Boolean state;
    private List<Integer> roles;
}
