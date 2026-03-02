package com.CLMTZ.Backend.model.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "tbgestionrolesroles", schema = "seguridad")
public class RoleManagementRole {
    @Id
    @Column(name = "idgrolrol")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer roleManagementRoleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idgrol", foreignKey = @ForeignKey(name = "fk_gestionrolrol_gestionrol"))
    private RoleManagement roleManagementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idrol", foreignKey = @ForeignKey(name = "fk_gestionrolrol_rol"))
    private Role roleId;

    @Column(name = "estado", nullable = false, columnDefinition = "boolean default true")
    private Boolean state = true;
}
