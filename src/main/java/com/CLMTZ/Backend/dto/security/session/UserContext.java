package com.CLMTZ.Backend.dto.security.session;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserContext implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer userId;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private List<String> roles;
    private boolean serverSynced;
    private Character accountState;
    private String dbUser;
    private String dbPassword; // Solo en memoria de sesión, nunca expuesto al cliente
    private Integer idAuditoriaAcceso; // Tambien se encontrara solo en memoria de la sesion, para no exponerlo a la sesion del cliente
    private Integer institutionId;
}
