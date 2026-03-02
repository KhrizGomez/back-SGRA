package com.CLMTZ.Backend.model.academic;

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
@Table(name = "tbdetallematricula", schema = "academico")
public class EnrollmentDetail {
    @Id
    @Column(name = "iddetallematricula")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer enrollmentDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idmatricula", foreignKey = @ForeignKey(name = "fk_detallematricula_matriculas"))
    private Registrations registrationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idasignatura", foreignKey = @ForeignKey(name = "fk_detallematricula_asignaturas"))
    private Subject subjectId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idparalelo", foreignKey = @ForeignKey(name = "fk_detallematricula_paralelo"))
    private Parallel parallelId;

    @Column(name = "estado", nullable = false, columnDefinition = ("boolean default true"))
    private Boolean active = true;
}