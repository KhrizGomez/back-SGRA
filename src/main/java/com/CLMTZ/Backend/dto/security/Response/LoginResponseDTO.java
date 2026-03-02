package com.CLMTZ.Backend.dto.security.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDTO {
    private Integer userId;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private List<String> roles;
    private boolean serverSynced;
    private Character accountState;
}
