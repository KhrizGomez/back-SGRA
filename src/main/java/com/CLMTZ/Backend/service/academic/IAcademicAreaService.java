package com.CLMTZ.Backend.service.academic;

import java.util.List;

import com.CLMTZ.Backend.dto.academic.AcademicAreaDTO;

public interface IAcademicAreaService {
    List<AcademicAreaDTO> findAll();
    AcademicAreaDTO findById(Integer id);
    AcademicAreaDTO save(AcademicAreaDTO dto);
    AcademicAreaDTO update(Integer id, AcademicAreaDTO dto);
    void deleteById(Integer id);
}
