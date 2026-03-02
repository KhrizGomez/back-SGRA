package com.CLMTZ.Backend.service.academic;

import java.util.List;

import com.CLMTZ.Backend.dto.academic.CoordinationDTO;
import com.CLMTZ.Backend.dto.academic.StudentLoadDTO;
import com.CLMTZ.Backend.dto.academic.TeachingDTO;

public interface ICoordinationService {
    List<CoordinationDTO> findAll();
    CoordinationDTO findById(Integer id);
    CoordinationDTO save(CoordinationDTO dto);
    CoordinationDTO update(Integer id, CoordinationDTO dto);
    void deleteById(Integer id);
    List<String> uploadStudents(List<StudentLoadDTO> dtos);
    List<String> uploadTeachers(List<TeachingDTO> dtos);
}