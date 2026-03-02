package com.CLMTZ.Backend.model.security;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "tbgestionusuarios", schema = "seguridad")
public class UserManagement {
    @Id
    @Column(name = "idgusuario")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer userGId;

    @Column(name = "usuario",length = 100, nullable = false, unique = true)
    private String user;

    @Column(name = "contrasena", nullable = false, columnDefinition = "TEXT")
    private String password;

    @Column(name = "estado", nullable = false, columnDefinition = "boolean default true")
    private Boolean state = true;

    @Column(name = "fechahoracreacion", nullable = false, columnDefinition = "timestamp default CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "userManagement", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<UserRoleManagement> userRoleManagements;

    @OneToOne(mappedBy = "userManagement", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private UserUserManagement userUserManagement;
}
