package com.CLMTZ.Backend.repository.academic;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.CLMTZ.Backend.model.academic.Parallel;

public interface IParallelRepository extends JpaRepository<Parallel, Integer> {

    Optional<Parallel> findBySectionIgnoreCase(String section);

}
