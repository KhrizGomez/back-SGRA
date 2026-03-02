package com.CLMTZ.Backend.repository.general;

import org.springframework.data.jpa.repository.JpaRepository;

import com.CLMTZ.Backend.model.general.Gender;

public interface IGenderRepository extends JpaRepository<Gender, Integer> {

}
