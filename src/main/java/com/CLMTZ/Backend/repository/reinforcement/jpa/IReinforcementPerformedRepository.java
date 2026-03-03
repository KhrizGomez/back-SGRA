package com.CLMTZ.Backend.repository.reinforcement.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.CLMTZ.Backend.model.reinforcement.ReinforcementPerformed;

public interface IReinforcementPerformedRepository extends JpaRepository<ReinforcementPerformed, Integer> {

}
