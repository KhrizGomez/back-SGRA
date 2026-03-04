package com.CLMTZ.Backend.model.reinforcement;

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
@Table(name = "tbasistenciasrefuerzos", schema = "reforzamiento")
public class AttendanceReinforcement {
    @Id
    @Column(name = "idasistencia")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer attendanceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idrefuerzoprogramado", nullable = false, foreignKey = @ForeignKey(name = "fk_asistencia_programado"))
    private ScheduledReinforcement scheduledPerformedId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idparticipante", nullable = false, foreignKey = @ForeignKey(name = "fk_asistencia_participante"))
    private Participants participants;

    @Column(name = "asistencia", nullable = false, columnDefinition = "boolean default false")
    private Boolean attendance = false;
}