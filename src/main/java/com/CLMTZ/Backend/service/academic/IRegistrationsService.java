package com.CLMTZ.Backend.service.academic;

import java.util.List;

import com.CLMTZ.Backend.dto.academic.RegistrationsDTO;

public interface IRegistrationsService {
    List<RegistrationsDTO> findAll();
    RegistrationsDTO findById(Integer id);
    RegistrationsDTO save(RegistrationsDTO dto);
    RegistrationsDTO update(Integer id, RegistrationsDTO dto);
    void deleteById(Integer id);
}
