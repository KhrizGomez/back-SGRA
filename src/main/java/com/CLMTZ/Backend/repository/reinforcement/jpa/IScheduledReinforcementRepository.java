package com.CLMTZ.Backend.repository.reinforcement.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.CLMTZ.Backend.model.reinforcement.ScheduledReinforcement;

public interface IScheduledReinforcementRepository extends JpaRepository<ScheduledReinforcement, Integer> {

}
