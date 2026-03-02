package com.CLMTZ.Backend.model.academic;

import java.util.List;

import com.CLMTZ.Backend.model.reinforcement.ReinforcementRequest;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
@Entity
@Table(name = "tbasignaturas", schema = "academico")
public class Subject {
    @Id
    @Column(name = "idasignatura")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer idSubject;

    @Column(name = "asignatura",nullable = false, columnDefinition = "TEXT")
    private String subject;

    @Column(name = "semestre",nullable = false, columnDefinition = "smallint")
    private Short semester;

    @Column(name = "estado",nullable = false,columnDefinition = "boolean default true")
    private Boolean state = true;

    @OneToMany(mappedBy = "subjectId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<SubjectsCareers> subjectsCareers;

    @OneToMany(mappedBy = "subjectId", fetch = FetchType.LAZY)
    private List<EnrollmentDetail> enrollmentDetails;

    @OneToMany(mappedBy = "subjectId", fetch = FetchType.LAZY)
    private List<Class> classes;

    @OneToMany(mappedBy = "subjectId", fetch = FetchType.LAZY)
    private List<ReinforcementRequest> reinforcementRequests;
}
