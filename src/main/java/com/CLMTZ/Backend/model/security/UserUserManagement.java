package com.CLMTZ.Backend.model.security;

import com.CLMTZ.Backend.model.general.User;

import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "tbusuariosgestionusuarios", schema = "seguridad")
public class UserUserManagement {
    @Id
    @Column(name = "idusuariogusuario")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer userUserGId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idusuario", foreignKey = @ForeignKey(name = "fk_usuariogestionusuario_usuario"))
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idgusuario", foreignKey = @ForeignKey(name = "fk_usuariogestionusuario_gestionusuario"))
    private UserManagement userManagement;

    @Column(name = "estado", nullable = false, columnDefinition = "boolean default true")
    private Boolean state = true;
}
