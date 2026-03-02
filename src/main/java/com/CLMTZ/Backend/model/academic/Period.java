package com.CLMTZ.Backend.model.academic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

import com.CLMTZ.Backend.model.reinforcement.ReinforcementRequest;

@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "tbperiodos", schema = "academico")
public class Period {
    @Id
    @Column(name = "idperiodo")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer periodId;

    @Column(name = "periodo", length = 10, nullable = false, unique = true)
    private String period;

    @Column(name = "fechainicio", nullable = false, columnDefinition = "date")
    private LocalDate startDate;

    @Column(name = "fechafin", nullable = false, columnDefinition = "date")
    private LocalDate endDate;

    @Column(name = "estado", nullable = false, columnDefinition = "boolean default true")
    private Boolean state = true;

    @OneToMany(mappedBy = "periodId", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true )
    private List<Registrations> registrations;

    @OneToMany(mappedBy = "periodId", fetch = FetchType.LAZY)
    private List<Class> classes;

    @OneToMany(mappedBy = "periodId", fetch = FetchType.LAZY)
    private List<ClassSchedule> classSchedules;

    @OneToMany(mappedBy = "periodId", fetch = FetchType.LAZY)
    private List<ReinforcementRequest> reinforcementRequests;
}