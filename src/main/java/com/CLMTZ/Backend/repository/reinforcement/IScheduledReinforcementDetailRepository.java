package com.CLMTZ.Backend.repository.reinforcement;

import org.springframework.data.jpa.repository.JpaRepository;

import com.CLMTZ.Backend.model.reinforcement.ScheduledReinforcementDetail;

public interface IScheduledReinforcementDetailRepository extends JpaRepository<ScheduledReinforcementDetail, Integer> {

}
