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

@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "tbauditoriaacceso", schema = "seguridad")
public class AccessAudit {
    @Id
    @Column(name = "idauditoriaacceso")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer accessAuditId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idusuario", foreignKey = @ForeignKey(name = "fk_auditacceso_usuario"))
    private User userId;

    @Column(name = "direccionip", length = 50, nullable = false)
    private String ipAddress;

    @Column(name = "navegador", nullable = false, columnDefinition = "TEXT")
    private String browser;

    @Column(name = "fechaacceso", nullable = false, columnDefinition = "timestamp default CURRENT_TIMESTAMP")
    private LocalDateTime accessDate;

    @Column(name = "so", length = 15, nullable = false)
    private String sO;

    @Column(name = "sesion", nullable = false, columnDefinition = "TEXT")
    private String session;

    @Column(name = "accion", length = 50, nullable = false)
    private String action;
}
