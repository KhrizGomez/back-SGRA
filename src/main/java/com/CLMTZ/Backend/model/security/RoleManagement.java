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
@Table(name = "tbgestionroles", schema = "seguridad")
public class RoleManagement {
    @Id
    @Column(name = "idgrol")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer roleGId;

    @Column(name = "rolservidor",length = 100, nullable = false)
    private String serverRole;

    @Column(name = "grol",length = 100, nullable = false)
    private String roleG;

    @Column(name = "descripcion", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "fechahoracreacion", nullable = false, columnDefinition = "timestamp default CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "estado", nullable = false, columnDefinition = "boolean default true")
    private Boolean state = true;

    @OneToMany(mappedBy = "roleManagement", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<UserRoleManagement> userRoleManagements;

    @OneToMany(mappedBy = "roleManagementId", fetch = FetchType.LAZY)
    private List<RoleManagementRole> roleManagementRoles;
}
