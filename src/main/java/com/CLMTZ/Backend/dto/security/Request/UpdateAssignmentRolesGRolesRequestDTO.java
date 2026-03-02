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
public class UpdateAssignmentRolesGRolesRequestDTO {
    private Integer roleAppGId;
    private String roleAppG;
    private List<Integer> serverRoleIds;
}
