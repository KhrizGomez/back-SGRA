package com.CLMTZ.Backend.repository.reinforcement.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import com.CLMTZ.Backend.model.reinforcement.ScheduledReinforcementStatus;

public interface IScheduledReinforcementStatusRepository extends JpaRepository<ScheduledReinforcementStatus, Integer> {

}
