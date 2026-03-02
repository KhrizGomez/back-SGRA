package com.CLMTZ.Backend.service.academic;

import java.util.List;

import com.CLMTZ.Backend.dto.academic.StudentsDTO;

public interface IStudentsService {
    List<StudentsDTO> findAll();
    StudentsDTO findById(Integer id);
    StudentsDTO save(StudentsDTO dto);
    StudentsDTO update(Integer id, StudentsDTO dto);
    void deleteById(Integer id);
}
