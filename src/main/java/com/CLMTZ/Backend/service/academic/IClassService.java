package com.CLMTZ.Backend.service.academic;

import java.util.List;

import com.CLMTZ.Backend.dto.academic.ClassDTO;

public interface IClassService {
    List<ClassDTO> findAll();
    ClassDTO findById(Integer id);
    ClassDTO save(ClassDTO dto);
    ClassDTO update(Integer id, ClassDTO dto);
    void deleteById(Integer id);
}
