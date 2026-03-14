package com.CLMTZ.Backend.repository.academic;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.CLMTZ.Backend.model.academic.Class;

public interface IClassRepository extends JpaRepository<Class, Integer> {
	Optional<Class> findByTeacherId_UserId_IdentificationAndSubjectId_SubjectIgnoreCaseAndParallelId_SectionIgnoreCaseAndPeriodId_PeriodIgnoreCase(
			String identification,
			String subject,
			String section,
			String period);

	/**
	 * Busca una clase por materia + paralelo en el periodo activo (state = true).
	 * Usado para detectar si la clase ya existe con un docente diferente al cargar el Excel.
	 */
	Optional<Class> findBySubjectId_SubjectIgnoreCaseAndParallelId_SectionIgnoreCaseAndPeriodId_StateTrue(
			String subject,
			String section);
}
