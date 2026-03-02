package com.CLMTZ.Backend.model.academic;

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
@Table(name = "tbparalelos", schema = "academico")
public class Parallel {
    @Id
    @Column(name = "idparalelo")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer parallelId;

    @Column(name = "paralelo", length = 5, nullable = false, columnDefinition = "char(5)")
    private String section;

    @Column(name = "estado", nullable = false, columnDefinition = ("boolean default true"))
    private Boolean active = true;

    @OneToMany(mappedBy = "parallelId", fetch = FetchType.LAZY)
    private List<EnrollmentDetail> enrollmentDetails;

    @OneToMany(mappedBy = "parallelId", fetch = FetchType.LAZY)
    private List<Class> classes;
}
