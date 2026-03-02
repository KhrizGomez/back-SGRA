package com.CLMTZ.Backend.model.security;

import java.time.LocalDateTime;

import com.CLMTZ.Backend.model.general.User;

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
@Table(name = "tbconfiguracionescorreos", schema = "seguridad")
public class EmailSettings {
    @Id
    @Column(name = "idconfiguracioncorreo")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer emailSettingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idusuario", foreignKey = @ForeignKey(name = "fk_configuracionemail_usuario"))
    private User userId;

    @Column(name = "correoemisor", nullable = false, columnDefinition = "text")
    private String emailSender;

    @Column(name = "contrasenaaplicacion", nullable = false, columnDefinition = "text")
    private String applicationPassword;

    @Column(name = "fechahoracreacion", nullable = false, columnDefinition = "timestamp default CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "estado", nullable = false, columnDefinition = "boolean default true")
    private Boolean state = true;
}
