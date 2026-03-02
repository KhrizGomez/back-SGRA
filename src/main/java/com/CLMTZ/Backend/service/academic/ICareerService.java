package com.CLMTZ.Backend.service.academic;

import java.util.List;

import com.CLMTZ.Backend.dto.academic.CareerDTO;
import com.CLMTZ.Backend.dto.academic.CareerLoadDTO;

public interface ICareerService {
    List<CareerDTO> findAll();
    CareerDTO findById(Integer id);
    CareerDTO save(CareerDTO dto);
    CareerDTO update(Integer id, CareerDTO dto);
    void deleteById(Integer id);
    List<String> uploadCareers(List<CareerLoadDTO> dtos);
}
