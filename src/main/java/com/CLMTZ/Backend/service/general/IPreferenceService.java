package com.CLMTZ.Backend.service.general;

import java.util.List;

import com.CLMTZ.Backend.dto.general.PreferenceDTO;

public interface IPreferenceService {
    List<PreferenceDTO> findAll();
    PreferenceDTO findById(Integer id);
    PreferenceDTO save(PreferenceDTO dto);
    PreferenceDTO update(Integer id, PreferenceDTO dto);
    void deleteById(Integer id);
}
