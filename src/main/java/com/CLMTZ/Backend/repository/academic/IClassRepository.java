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
}
