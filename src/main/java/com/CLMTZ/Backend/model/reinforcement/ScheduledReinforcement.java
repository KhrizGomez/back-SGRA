package com.CLMTZ.Backend.model.reinforcement;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.CLMTZ.Backend.model.academic.Modality;
import com.CLMTZ.Backend.model.academic.TimeSlot;

@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "tbrefuerzosprogramados", schema = "reforzamiento")
public class ScheduledReinforcement {
    @Id
    @Column(name = "idrefuerzoprogramado")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer scheduledReinforcementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idtiposesion", foreignKey = @ForeignKey(name = "fk_refuerzoprogramado_tiposesion"))
    private SessionTypes sessionTypeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idmodalidad", foreignKey = @ForeignKey(name = "fk_refuerzoprogramado_modalidad"))
    private Modality modalityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idfranjahoraria", foreignKey = @ForeignKey(name = "fk_refuerzoprogramado_franjahoraria"))
    private TimeSlot timeSlotId;

    @Column(name = "fechaprogramadarefuerzo", nullable = false, columnDefinition = "date")
    private LocalDate scheduledDateReinforcement;

    @Column(name = "duracionestimado", nullable = false, columnDefinition = "time")
    private LocalTime estimatedTime;
    
    @Column(name = "motivo", length = 200)
    private String reason;

    @Column (name = "fechacreacion",nullable = false, columnDefinition = "timestamp")
    private LocalDateTime newSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idestadorefuerzoprogramado", foreignKey = @ForeignKey(name = "fk_refuerzoprogramado_estadorefuerzoprogramado"))
    private ScheduledReinforcementStatus scheduledReinforcementStatus;

    @OneToMany(mappedBy = "scheduledReinforcementId", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScheduledReinforcementDetail> scheduledReinforcementDetails;

    @OneToMany(mappedBy = "scheduledReinforcementId", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReinforcementPerformed> reinforcementsPerformed;

    @OneToMany(mappedBy = "scheduledReinforcementId", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OnSiteReinforcement> onSiteReinforcements;

    @OneToMany(mappedBy = "scheduledReinforcement", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScheduledReinforcementResources> scheduledReinforcementResources;

    @OneToMany(mappedBy = "scheduledPerformedId", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AttendanceReinforcement> attendanceReinforcements;
}