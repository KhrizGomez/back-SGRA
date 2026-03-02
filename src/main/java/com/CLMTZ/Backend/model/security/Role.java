package com.CLMTZ.Backend.model.security;

import java.util.List;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "tbroles", schema = "seguridad")
public class Role {
    @Id
    @Column(name = "idrol", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer roleId;

    @Column(name = "rol", length = 27, nullable = false)
    private String role;

    @Column(name = "estado", nullable = false, columnDefinition = "boolean default true")
    private Boolean state = true;

    @OneToMany(mappedBy = "roleId", fetch = FetchType.LAZY)
    private List<UsersRoles> usersRoles;

    @OneToMany(mappedBy = "roleId", fetch = FetchType.LAZY)
    private List<RoleManagementRole> roleManagementRoles;
}