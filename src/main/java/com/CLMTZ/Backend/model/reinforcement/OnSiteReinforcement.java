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
@Table(name = "tbrefuerzospresenciales", schema = "reforzamiento")
public class OnSiteReinforcement {
    @Id
    @Column(name = "idrefuerzopresencial")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer onSiteReinforcementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idtipoareatrabajo", nullable = true, foreignKey = @ForeignKey(name = "fk_presencial_tipoareatrabajo"))
    private WorkAreaTypes workAreaTypeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idareatrabajo", nullable = true, foreignKey = @ForeignKey(name = "fk_presencial_areatrabajo"))
    private WorkArea workAreaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idrefuerzoprogramado", nullable = false, foreignKey = @ForeignKey(name = "fk_presencial_programado"))
    private ScheduledReinforcement scheduledReinforcementId;

    @Column(name = "estado", nullable = false, columnDefinition = "boolean default true")
    private Boolean state = true;
}