package com.CLMTZ.Backend.model.academic;

import java.util.List;

import com.CLMTZ.Backend.model.general.Institution;
import com.CLMTZ.Backend.model.reinforcement.WorkArea;
import com.CLMTZ.Backend.model.reinforcement.WorkAreaManager;

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
@Table(name = "tbareasacademicas", schema = "academico")
public class AcademicArea {
    @Id
    @Column(name = "idareaacademica")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer areaAcademicId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idinstitucion", foreignKey = @ForeignKey(name = "fk_tbareasacademicas_tbinstituciones"))
    private Institution institutionId;

    @Column(name = "nombre",length = 200,nullable = false)
    private String nameArea;

    @Column(name = "abreviatura",length = 10,nullable = false, columnDefinition="char(10)")
    private String abbreviation;

    @Column(name = "ubicacion", length = 150,nullable = false)
    private String location;

    @Column(name = "estado", nullable = false, columnDefinition = "boolean default true")
    private Boolean state = true;

    @OneToMany(mappedBy = "academicAreaId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Career> careers;

    @OneToMany(mappedBy = "areaAcademicId", fetch = FetchType.LAZY)
    private List<WorkArea> workAreas;

    @OneToMany(mappedBy = "areaAcademicId", fetch = FetchType.LAZY)
    private List<WorkAreaManager> workAreaManagers;
}
