package com.CLMTZ.Backend.repository.reinforcement.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import com.CLMTZ.Backend.model.reinforcement.ScheduledReinforcementResources;

public interface IScheduledReinforcementResourcesRepository extends JpaRepository<ScheduledReinforcementResources, Integer> {

}
