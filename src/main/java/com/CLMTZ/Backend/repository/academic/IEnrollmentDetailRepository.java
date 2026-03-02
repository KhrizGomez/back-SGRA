package com.CLMTZ.Backend.repository.academic;

import org.springframework.data.jpa.repository.JpaRepository;

import com.CLMTZ.Backend.model.academic.EnrollmentDetail;

public interface IEnrollmentDetailRepository extends JpaRepository<EnrollmentDetail, Integer> {

}
