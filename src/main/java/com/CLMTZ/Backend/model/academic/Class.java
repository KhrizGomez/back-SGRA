package com.CLMTZ.Backend.model.academic;

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
@Table(name = "tbclases", schema = "academico")
public class Class {
    @Id
    @Column(name = "idclase")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer idClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "iddocente", foreignKey = @ForeignKey(name = "fk_clases_docentes"))
    private Teaching teacherId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idasignatura", foreignKey = @ForeignKey(name = "fk_clases_asignaturas"))
    private Subject subjectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idperiodo", foreignKey = @ForeignKey(name = "fk_clases_periodos"))
    private Period periodId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idparalelo", foreignKey = @ForeignKey(name = "fk_detallematricula_paralelo"))
    private Parallel parallelId;

    @Column(name = "estado", nullable = false, columnDefinition = "boolean default true")
    private Boolean active = true;

    @OneToMany(mappedBy = "assignedClassId", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClassSchedule> classSchedules;
}
