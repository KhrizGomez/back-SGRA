package com.CLMTZ.Backend.model.reinforcement;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "tbestadosrefuerzosprogramados", schema = "reforzamiento")
public class ScheduledReinforcementStatus {
    @Id
    @Column(name = "idestadorefuerzoprogramado")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer scheduledReinforcementStatusId;

    @Column(name = "estadorefuerzoprogramado", length = 15,nullable = false, unique = true)
    private String ScheduledReinforcementStatus;

    @Column(name = "estado", nullable = false, columnDefinition = "boolean default true")
    private Boolean state = true;

    @OneToMany(mappedBy = "scheduledReinforcementStatus", fetch = FetchType.LAZY)
    private List<ScheduledReinforcement> scheduledReinforcementId;
}
