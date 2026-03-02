package com.CLMTZ.Backend.model.reinforcement;

import java.util.List;

import com.CLMTZ.Backend.model.academic.Students;

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
@Table(name = "tbparticipantes", schema = "reforzamiento")
public class Participants {
    @Id
    @Column(name = "idparticipante")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer participantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idestudiante", nullable = false, foreignKey = @ForeignKey(name = "fk_participantes_estudiante"))
    private Students studentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idsolicitudrefuerzo", nullable = false, foreignKey = @ForeignKey(name = "fk_participantes_solicitud"))
    private ReinforcementRequest reinforcementRequestId;

    @Column(name = "participacion", nullable = false, columnDefinition = "boolean default false")
    private Boolean stake = false;

    @OneToMany(mappedBy = "participants", fetch = FetchType.LAZY)
    private List<AttendanceReinforcement> attendanceReinforcements;
}