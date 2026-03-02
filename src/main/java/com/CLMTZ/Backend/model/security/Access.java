package com.CLMTZ.Backend.model.security;

import com.CLMTZ.Backend.model.general.User;

import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "tbaccesos", schema = "seguridad")
public class Access {
    @Id
    @Column(name = "idaccesso")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer accessId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idusuario", foreignKey = @ForeignKey(name = "fk_acceso_usuario"))
    private User user;

    @Column(name = "nombreusuario", length = 50, nullable = false)
    private String username;

    @Column(name = "contrasena", nullable = false, columnDefinition = "TEXT")
    private String password;

    @Column(name = "estado", nullable = false, columnDefinition = "char(1) default 'A' check (estado in ('A', 'I', 'C'))")
    private Character state = 'A';
}
