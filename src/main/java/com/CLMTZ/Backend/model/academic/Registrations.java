package com.CLMTZ.Backend.model.academic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "tbmatriculas", schema = "academico")
public class Registrations {
    @Id
    @Column(name = "idmatricula")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer registrationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idperiodo", foreignKey = @ForeignKey(name = "fk_matriculas_periodos"))
    private Period periodId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idestudiante", foreignKey = @ForeignKey(name = "fk_matriculas_estudiantes"))
    private Students studentId;

    @Column(name = "fechainscripcion", nullable = false, columnDefinition = "date")
    private LocalDate date;

    @Column(name = "estado", nullable = false, columnDefinition = "boolean default true")
    private Boolean status = true;

    @OneToMany(mappedBy = "registrationId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EnrollmentDetail> enrollmentDetails;
}