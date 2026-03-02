package com.CLMTZ.Backend.repository.reinforcement;

import org.springframework.data.jpa.repository.JpaRepository;

import com.CLMTZ.Backend.model.reinforcement.AttendanceReinforcement;

public interface IAttendanceReinforcementRepository extends JpaRepository<AttendanceReinforcement, Integer> {

}
