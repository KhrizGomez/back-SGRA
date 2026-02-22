package com.CLMTZ.Backend.service.academic;

import java.util.List;

import com.CLMTZ.Backend.dto.academic.SyllabiDTO;
import com.CLMTZ.Backend.dto.academic.SyllabiLoadDTO;

public interface ISyllabiService {
    List<SyllabiDTO> findAll();
    SyllabiDTO findById(Integer id);
    SyllabiDTO save(SyllabiDTO dto);
    SyllabiDTO update(Integer id, SyllabiDTO dto);
    void deleteById(Integer id);
    List<String> uploadSyllabi(List<SyllabiLoadDTO> syllabiDTOs);
}
