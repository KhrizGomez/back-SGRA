package com.CLMTZ.Backend.service.academic;

import java.util.List;

import com.CLMTZ.Backend.dto.academic.SubjectsCareersDTO;

public interface ISubjectsCareersService {
    List<SubjectsCareersDTO> findAll();
    SubjectsCareersDTO findById(Integer id);
    SubjectsCareersDTO save(SubjectsCareersDTO dto);
    SubjectsCareersDTO update(Integer id, SubjectsCareersDTO dto);
    void deleteById(Integer id);
}
