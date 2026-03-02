package com.CLMTZ.Backend.service.academic;

import java.util.List;

import com.CLMTZ.Backend.dto.academic.ModalityDTO;

public interface IModalityService {
    List<ModalityDTO> findAll();
    ModalityDTO findById(Integer id);
    ModalityDTO save(ModalityDTO dto);
    ModalityDTO update(Integer id, ModalityDTO dto);
    void deleteById(Integer id);
}
