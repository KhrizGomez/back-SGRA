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
@Table(name = "tbdetallesrefuerzosprogramadas", schema = "reforzamiento")
public class ScheduledReinforcementDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "iddetallerefuerzoprogramado", nullable = false)
    @EqualsAndHashCode.Include
    private Integer scheduledReinforcementDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idrefuerzoprogramado", foreignKey = @ForeignKey(name = "fk_detallerefuerzoprogramado_refuerzoprogramado"))
    private ScheduledReinforcement scheduledReinforcementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idsolicitudrefuerzo", foreignKey = @ForeignKey(name = "fk_detallerefuerzoprogramado_solicitudrefuerzo"))
    private ReinforcementRequest reinforcementRequestId;

    @Column(name = "estado", nullable = false, columnDefinition = "boolean default true")
    private Boolean state = true;
}
