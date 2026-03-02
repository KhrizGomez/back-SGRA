package com.CLMTZ.Backend.repository.general;

import org.springframework.data.jpa.repository.JpaRepository;

import com.CLMTZ.Backend.model.general.Preference;

public interface IPreferenceRepository extends JpaRepository<Preference, Integer> {

}
