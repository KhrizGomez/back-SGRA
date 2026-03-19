package com.CLMTZ.Backend.model.reinforcement;

import com.CLMTZ.Backend.model.academic.Period;
import com.CLMTZ.Backend.model.academic.TimeSlot;
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
@Table(name = "tbdisponibilidaddocente", schema = "reforzamiento")
public class TeacherAvailability {

    @Id
    @Column(name = "iddisponibilidad")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer teacherAvailabilityId;

    @Column(name = "iddocente", nullable = false)
    private Integer teacherId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idperiodo")
    private Period period;

    @Column(name = "diasemana", nullable = false)
    private Short dayOfWeek;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idfranjahoraria")
    private TimeSlot timeSlot;

    @Column(name = "estado", nullable = false, columnDefinition = "boolean default true")
    private Boolean state = true;
}