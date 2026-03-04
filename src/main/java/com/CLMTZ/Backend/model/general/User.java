package com.CLMTZ.Backend.model.general;

import java.util.List;

import com.CLMTZ.Backend.model.academic.Coordination;
import com.CLMTZ.Backend.model.academic.Students;
import com.CLMTZ.Backend.model.academic.Teaching;
import com.CLMTZ.Backend.model.reinforcement.WorkAreaManager;
import com.CLMTZ.Backend.model.security.Access;
import com.CLMTZ.Backend.model.security.AccessAudit;
import com.CLMTZ.Backend.model.security.EmailSettings;
import com.CLMTZ.Backend.model.security.UserUserManagement;
import com.CLMTZ.Backend.model.security.UsersRoles;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "tbusuarios", schema = "general")
public class User {
    @Id
    @Column(name = "idusuario")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer userId;

    @Column(name = "nombres", length = 100, nullable = false)
    private String firstName;

    @Column(name = "apellidos", length = 100, nullable = false)
    private String lastName;

    @Column(name = "identificador", length = 13, nullable = false, unique = true)
    private String identification;

    @Column(name = "telefono", length = 10, nullable = false, columnDefinition = "char(10)", unique = true)
    private String phoneNumber;

    @Column(name = "correo", length = 200, nullable = false, unique = true)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idinstitucion", foreignKey = @ForeignKey(name = "fk_usuario_institucion"))
    private Institution institutionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idgenero", foreignKey = @ForeignKey(name = "fk_usuario_genero"))
    private Gender idGender;

    @Column(name = "direccion", length = 200, nullable = false)
    private String address;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Access access;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private UserUserManagement userUserManagement;

    @OneToMany(mappedBy = "userId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<AccessAudit> accessAudit;

    @OneToMany(mappedBy = "userId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<UsersRoles> usersRoles;

    @OneToOne(mappedBy = "userId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Students student;

    @OneToOne(mappedBy = "userId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Teaching teaching;

    @OneToOne(mappedBy = "userId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Coordination coordination;

    @OneToOne(mappedBy = "userId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private WorkAreaManager workAreaManager;

    @OneToMany(mappedBy = "userId", fetch = FetchType.LAZY)
    private List<Preference> preferences;

    @OneToMany(mappedBy = "userId", fetch = FetchType.LAZY)
    private List<Notification> notifications;

    @OneToMany(mappedBy = "userId", fetch = FetchType.LAZY)
    private List<EmailSettings> emailSettings;
}
