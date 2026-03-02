package com.CLMTZ.Backend.service.academic;

import java.util.List;

import com.CLMTZ.Backend.dto.academic.TimeSlotDTO;

public interface ITimeSlotService {
    List<TimeSlotDTO> findAll();
    TimeSlotDTO findById(Integer id);
    TimeSlotDTO save(TimeSlotDTO dto);
    TimeSlotDTO update(Integer id, TimeSlotDTO dto);
    void deleteById(Integer id);
}
