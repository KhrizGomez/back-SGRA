package com.CLMTZ.Backend.dto.security.Request;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleManagementRequestDTO {
    private Integer roleGId;
    private String roleG;
    private String serverRole;
    private String description;
    private LocalDateTime createdAt;
    private Boolean state;
}
