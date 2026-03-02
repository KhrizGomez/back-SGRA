package com.CLMTZ.Backend.repository.academic;

import org.springframework.data.jpa.repository.JpaRepository;

import com.CLMTZ.Backend.model.academic.Students;

public interface IStudentsRepository extends JpaRepository<Students, Integer> {

}
