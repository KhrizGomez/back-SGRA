package com.CLMTZ.Backend.service.academic;

import java.util.List;

import com.CLMTZ.Backend.dto.academic.ParallelDTO;

public interface IParallelService {
    List<ParallelDTO> findAll();
    ParallelDTO findById(Integer id);
    ParallelDTO save(ParallelDTO dto);
    ParallelDTO update(Integer id, ParallelDTO dto);
    void deleteById(Integer id);
}
