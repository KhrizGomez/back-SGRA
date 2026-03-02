package com.CLMTZ.Backend.repository.academic;

import org.springframework.data.jpa.repository.JpaRepository;

import com.CLMTZ.Backend.model.academic.Registrations;

public interface IRegistrationsRepository extends JpaRepository<Registrations, Integer> {

}
