package com.CLMTZ.Backend.repository.academic;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.CLMTZ.Backend.model.academic.Students;

public interface IStudentsRepository extends JpaRepository<Students, Integer> {

    Optional<Students> findByUserId_Identification(String identification);

}
