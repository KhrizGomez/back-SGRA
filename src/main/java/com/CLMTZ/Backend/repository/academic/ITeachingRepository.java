package com.CLMTZ.Backend.repository.academic;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

import com.CLMTZ.Backend.model.academic.Teaching;

public interface ITeachingRepository extends JpaRepository<Teaching, Integer> {
    Optional<Teaching> findByUserId_UserId(Integer userId);
}
