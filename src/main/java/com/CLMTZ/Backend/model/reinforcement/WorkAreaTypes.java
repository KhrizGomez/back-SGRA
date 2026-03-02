package com.CLMTZ.Backend.model.reinforcement;

import java.util.List;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "tbtiposareastrabajos", schema = "reforzamiento")
public class WorkAreaTypes {
    @Id
    @Column(name = "idtipoareatrabajo")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer workAreaTypeId;

    @Column(name = "tipoareatrabajo", nullable = false, columnDefinition = "TEXT")
    private String workAreaType;

    @Column(name = "estado", nullable = false, columnDefinition = "boolean default true")
    private Boolean state = true;

    @OneToMany(mappedBy = "workAreaTypeId", fetch = FetchType.LAZY)
    private List<WorkArea> workAreas;

    @OneToMany(mappedBy = "workAreaTypeId", fetch = FetchType.LAZY)
    private List<OnSiteReinforcement> onSiteReinforcements;
}