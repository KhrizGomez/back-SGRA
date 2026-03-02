package com.CLMTZ.Backend.model.reinforcement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "tbrecursosrefuerzosprogramados", schema = "reforzamiento")
public class ScheduledReinforcementResources {
    @Id
    @Column(name = "idrecursorefuerzoprogramado", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer scheduledReinforcementResourcesId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idrefuerzoprogramado", foreignKey = @ForeignKey(name = "fk_recursosolicitudrefuerzo_refuerzoprogramado"))
    private ScheduledReinforcement scheduledReinforcement;

    @Column(name = "urlarchivorefuerzoprogramado", nullable = false, columnDefinition = "TEXT")
    private String urlFileScheduledReinforcement;
}
