package com.CLMTZ.Backend.model.security;

import com.CLMTZ.Backend.model.general.User;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "tbusuariosroles", schema = "seguridad")
public class UsersRoles {
    @Id
    @Column(name = "idusuariorol")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer userRolesId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idrol", foreignKey = @ForeignKey(name = "fk_usuariorol_rol"))
    private Role roleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idusuario", foreignKey = @ForeignKey(name = "fk_usuariorol_usuario"))
    private User userId;

    @Column(name = "estado", nullable = false, columnDefinition = "boolean default true")
    private Boolean state = true;
}
