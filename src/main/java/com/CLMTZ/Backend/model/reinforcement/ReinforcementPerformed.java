package com.CLMTZ.Backend.model.reinforcement;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.sql.Time;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "tbrefuerzosrealizados", schema = "reforzamiento")
public class ReinforcementPerformed {
    @Id
    @Column(name = "idrefuerzorealizado")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer reinforcementPerformedId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idrefuerzoprogramado", nullable = false, foreignKey = @ForeignKey(name = "fk_refuerzorealizado_refuerzoprogramado"))
    private ScheduledReinforcement scheduledReinforcementId;

    @Column(name = "observacion",nullable = false, columnDefinition = "TEXT")
    private String observation;

    @Column(name = "duracion", nullable = false, columnDefinition = "time")
    private Time duration;
    
    @Column(name = "estado",nullable = false, columnDefinition = "char(1) default 'E' check (estado in ('F', 'E', 'I'))")
    private Character state;

    @OneToMany(mappedBy = "reinforcementPerformedId", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AttendanceReinforcement> attendanceReinforcements;
}
