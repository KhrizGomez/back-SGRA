package com.CLMTZ.Backend.repository.academic;

import java.time.LocalTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.CLMTZ.Backend.model.academic.TimeSlot;

public interface ITimeSlotRepository extends JpaRepository<TimeSlot, Integer> {
	Optional<TimeSlot> findByStartTimeAndEndTime(LocalTime startTime, LocalTime endTime);
}
