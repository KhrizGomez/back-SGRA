package com.CLMTZ.Backend.model.reinforcement;

import java.util.List;

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
@Table(name = "tbestadossolicitudesrefuerzos", schema = "reforzamiento")
public class ReinforcementRequestStatus {
    @Id
    @Column(name = "idestadosolicitudrefuerzo", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer reinforcementRequestStatusId;

    @Column(name = "nombreestado",length = 15,nullable = false, unique = true)
    private String nameState;

    @Column(name = "estado", nullable = false,columnDefinition = "boolean default true")
    private Boolean state = true;

    @OneToMany(mappedBy = "requestStatusId", fetch = FetchType.LAZY)
    private List<ReinforcementRequest> reinforcementRequests;
}
