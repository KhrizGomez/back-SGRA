package com.CLMTZ.Backend.repository.general;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.CLMTZ.Backend.model.general.Preference;

public interface IPreferenceRepository extends JpaRepository<Preference, Integer> {

    Optional<Preference> findByUserId_UserId(Integer userId);
}
