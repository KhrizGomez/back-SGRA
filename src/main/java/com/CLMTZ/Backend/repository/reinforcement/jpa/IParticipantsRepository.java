package com.CLMTZ.Backend.repository.reinforcement.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import com.CLMTZ.Backend.model.reinforcement.Participants;

public interface IParticipantsRepository extends JpaRepository<Participants, Integer> {

}
