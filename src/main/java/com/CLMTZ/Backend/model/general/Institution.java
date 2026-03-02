package com.CLMTZ.Backend.model.general;

import java.util.List;

import com.CLMTZ.Backend.model.academic.AcademicArea;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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
@Table(name = "tbinstituciones", schema = "general")
public class Institution {
    @Id
    @Column(name = "idinstitucion")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer institutionId;

    @Column(name = "nombreinstitucion",nullable = false, columnDefinition = "TEXT")
    private String nameInstitution;

    @Column(name = "estado",nullable = false, columnDefinition = "boolean default true")
    private Boolean state = true;

    @OneToMany(mappedBy = "institutionId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<AcademicArea> academicsAreas;

    @OneToMany(mappedBy = "institutionId", fetch = FetchType.LAZY)
    private List<User> users;
}
