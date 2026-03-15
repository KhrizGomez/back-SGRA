package com.CLMTZ.Backend.repository.academic;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.CLMTZ.Backend.model.academic.EnrollmentDetail;

public interface IEnrollmentDetailRepository extends JpaRepository<EnrollmentDetail, Integer> {

    /**
     * Busca un detalle de matrícula en el periodo activo por cédula de estudiante
     * y nombre de asignatura (case-insensitive).
     * Usado para actualizar el paralelo cuando cambia en carga masiva.
     */
    @Query("SELECT ed FROM EnrollmentDetail ed " +
           "WHERE ed.registrationId.studentId.userId.identification = :identificacion " +
           "AND UPPER(TRIM(ed.subjectId.subject)) = UPPER(TRIM(:asignatura)) " +
           "AND ed.registrationId.periodId.state = true")
    Optional<EnrollmentDetail> findByEstudianteYAsignaturaEnPeriodoActivo(
            @Param("identificacion") String identificacion,
            @Param("asignatura") String asignatura);

}
