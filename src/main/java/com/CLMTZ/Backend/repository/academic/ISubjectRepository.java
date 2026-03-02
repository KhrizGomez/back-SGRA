package com.CLMTZ.Backend.repository.academic;

import org.springframework.data.jpa.repository.JpaRepository;

import com.CLMTZ.Backend.model.academic.Subject;

public interface ISubjectRepository extends JpaRepository<Subject, Integer> {

}
