package com.CLMTZ.Backend.model.security;

import java.time.LocalDateTime;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
@Table(name = "tbauditoriadatos", schema = "seguridad")
public class DataAudits {
    @Id
    @Column(name = "idauditoriadato")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer dataAuditId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idusuario", foreignKey = @ForeignKey(name = "fk_auditdatos_usuario"))
    private User userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idauditoriaacceso", foreignKey = @ForeignKey(name = "fk_auditdatos_auditacceso"))
    private AccessAudit accessAuditId;

    @Column(name = "tablaafectada", length = 100, nullable = false)
    private String affectedTable;

    @Column(name = "fechahora", nullable = false, columnDefinition = "timestamp default CURRENT_TIMESTAMP")
    private LocalDateTime dateTime;

    @Column(name = "accion", length = 50, nullable = false)
    private String action;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datosnuevos", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> newData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datosantiguos", columnDefinition = "jsonb")
    private Map<String, Object> oldData;
}
