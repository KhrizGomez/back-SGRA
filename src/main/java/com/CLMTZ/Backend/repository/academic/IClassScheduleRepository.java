package com.CLMTZ.Backend.repository.academic;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.CLMTZ.Backend.model.academic.ClassSchedule;

public interface IClassScheduleRepository extends JpaRepository<ClassSchedule, Integer> {
	boolean existsByAssignedClassId_IdClassAndPeriodId_PeriodIdAndDayAndTimeSlotId_TimeSlotId(
			Integer classId,
			Integer periodId,
			Short day,
			Integer timeSlotId);

	List<ClassSchedule> findByAssignedClassId_TeacherId_UserId_UserId(Integer userId);
}
