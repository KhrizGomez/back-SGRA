package com.CLMTZ.Backend.service.academic;

import java.util.List;

import com.CLMTZ.Backend.dto.academic.EnrollmentDetailDTO;
import com.CLMTZ.Backend.dto.academic.EnrollmentDetailLoadDTO;

public interface IEnrollmentDetailService {
    List<EnrollmentDetailDTO> findAll();
    EnrollmentDetailDTO findById(Integer id);
    EnrollmentDetailDTO save(EnrollmentDetailDTO dto);
    EnrollmentDetailDTO update(Integer id, EnrollmentDetailDTO dto);
    void deleteById(Integer id);
    List<String> uploadEnrollmentDetails(List<EnrollmentDetailLoadDTO> registrationDTOs);
}
