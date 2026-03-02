package com.CLMTZ.Backend.service.general;

import java.util.List;

import com.CLMTZ.Backend.dto.general.InstitutionDTO;

public interface IInstitutionService {
    List<InstitutionDTO> findAll();
    InstitutionDTO findById(Integer id);
    InstitutionDTO save(InstitutionDTO dto);
    InstitutionDTO update(Integer id, InstitutionDTO dto);
    void deleteById(Integer id);
}
