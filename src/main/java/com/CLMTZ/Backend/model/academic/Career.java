package com.CLMTZ.Backend.model.academic;

import java.util.List;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
@Entity
@Table(name = "tbcarreras", schema = "academico")
public class Career {
    @Id
    @Column(name = "idcarrera")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer careerId;

    @Column(name = "nombrecarrera", length = 200, nullable = false, unique = true)
    private String nameCareer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idareaacademica", foreignKey = @ForeignKey(name = "fk_carrera_areacademica"))
    private AcademicArea academicAreaId;

    @Column(name = "semestres", nullable = false, columnDefinition = "smallint")
    private short semester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idmodalidad", foreignKey = @ForeignKey(name = "fk_carrera_modalidad"))
    private Modality modalityId;

    @Column(name = "estado", nullable = false,columnDefinition = "boolean default true")
    private Boolean state = true;

    @OneToMany(mappedBy = "careerId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<SubjectsCareers> subjectsCareers;

    @OneToMany(mappedBy = "careerId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Students> students;

    @OneToMany(mappedBy = "careerId", fetch = FetchType.LAZY)
    private List<Coordination> coordinations;
}