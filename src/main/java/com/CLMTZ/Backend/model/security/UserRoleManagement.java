package com.CLMTZ.Backend.model.security;

import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "tbgestionusuariosroles", schema = "seguridad")
public class UserRoleManagement {
    @Id
    @Column(name = "idgusuariorol")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer userRoleGId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idgusuario", foreignKey = @ForeignKey(name = "fk_gestionusuariorol_gestionusuario"))
    private UserManagement userManagement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idgrol", foreignKey = @ForeignKey(name = "fk_gestionusuariorol_gestionrol"))
    private RoleManagement roleManagement;

    @Column(name = "estado", nullable = false, columnDefinition = "boolean default true")
    private Boolean state = true;
}
