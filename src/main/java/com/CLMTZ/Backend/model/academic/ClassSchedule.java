package com.CLMTZ.Backend.model.academic;

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
@Table(name = "tbhorarioclases", schema = "academico")
public class ClassSchedule {
    @Id
    @Column(name = "idhorarioclases")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer idClassSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idfranjahorario", foreignKey = @ForeignKey(name = "fk_horarioclases_franjas"))
    private TimeSlot timeSlotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idclases", foreignKey = @ForeignKey(name = "fk_horarioclases_clases"))
    private Class assignedClassId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idperiodo", foreignKey = @ForeignKey(name = "fk_horarioclases_periodos"))
    private Period periodId;

    @Column(name = "dia", nullable = false, columnDefinition = "smallint")
    private Short day;

    @Column(name = "estado", nullable = false, columnDefinition = "boolean default true")
    private Boolean active = true;
}