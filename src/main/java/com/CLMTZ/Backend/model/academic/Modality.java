package com.CLMTZ.Backend.model.academic;

import java.util.List;

import com.CLMTZ.Backend.model.reinforcement.ScheduledReinforcement;

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
@Table(name = "tbmodalidades", schema = "academico")
public class Modality {
    @Id
    @Column(name = "idmodalidad")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer idModality;

    @Column(name = "modalidad", length = 15, nullable = false)
    private String modality;

    @Column(name = "estado", nullable = false, columnDefinition = "boolean default true")
    private Boolean state = true;

    @OneToMany(mappedBy = "modalityId", fetch = FetchType.LAZY)
    private List<Career> careers;

    @OneToMany(mappedBy = "modalityId", fetch = FetchType.LAZY)
    private List<ScheduledReinforcement> scheduledReinforcements;
}

