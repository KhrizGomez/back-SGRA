package com.CLMTZ.Backend.service.general;

import java.util.List;

import com.CLMTZ.Backend.dto.general.GenderDTO;

public interface IGenderService {
    List<GenderDTO> findAll();
    GenderDTO findById(Integer id);
    GenderDTO save(GenderDTO dto);
    GenderDTO update(Integer id, GenderDTO dto);
    void deleteById(Integer id);
}
