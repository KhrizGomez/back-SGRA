package com.CLMTZ.Backend.model.security;

import com.CLMTZ.Backend.model.general.Institution;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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
@Table(name = "tblogoinstituciones", schema = "seguridad")
public class InstitutionLogo {
    @Id
    @Column(name = "idlogoinstitucion")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer institutionLogoId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idinstitucion", foreignKey = @ForeignKey(name = "fk_logoinstitucion_institucion"))
    private Institution institutionId;

    @Column(name = "urllogo", nullable = false, columnDefinition = "TEXT")
    private String logoUrl;

    @Column(name = "estado", nullable = false, columnDefinition = "boolean default true")
    private Boolean state =true;
}
