package com.CLMTZ.Backend.repository.reinforcement;

import org.springframework.data.jpa.repository.JpaRepository;

import com.CLMTZ.Backend.model.reinforcement.ReinforcementRequestStatus;

public interface IReinforcementRequestStatusRepository extends JpaRepository<ReinforcementRequestStatus, Integer> {

}
