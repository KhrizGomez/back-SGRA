package com.CLMTZ.Backend.repository.reinforcement;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.CLMTZ.Backend.model.reinforcement.ReinforcementRequest;

public interface IReinforcementRequestRepository extends JpaRepository<ReinforcementRequest, Integer> {

    List<ReinforcementRequest> findByRequestStatusId_ReinforcementRequestStatusId(Integer statusId);
}
