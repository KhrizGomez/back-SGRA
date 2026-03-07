package com.CLMTZ.Backend.service.academic.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.CLMTZ.Backend.dto.academic.PeriodCUDDTO;
import com.CLMTZ.Backend.dto.academic.PeriodDTO;
import com.CLMTZ.Backend.dto.academic.PeriodLoadDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;
import com.CLMTZ.Backend.model.academic.Period;
import com.CLMTZ.Backend.repository.academic.IPeriodRepository;
import com.CLMTZ.Backend.repository.academic.custom.IPeriodCustomRepository;
import com.CLMTZ.Backend.service.academic.IPeriodService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PeriodServiceImpl implements IPeriodService {

    private final IPeriodRepository repository;
    private final IPeriodCustomRepository periodCustomRepo;

    @Override
    @Transactional(readOnly = true)
    public List<PeriodDTO> findAll() {
        return repository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public PeriodDTO findById(Integer id) {
        return repository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Period not found with id: " + id));
    }

    @Override
    public PeriodDTO save(PeriodDTO dto) {
        Period entity = new Period();
        entity.setPeriod(dto.getPeriodo());
        entity.setStartDate(dto.getFechainicio());
        entity.setEndDate(dto.getFechafin());
        entity.setState(dto.getEstado() != null ? dto.getEstado() : true);
        return toDTO(repository.save(entity));
    }

    @Override
    public PeriodDTO update(Integer id, PeriodDTO dto) {
        Period entity = repository.findById(id).orElseThrow(() -> new RuntimeException("Period not found with id: " + id));
        entity.setPeriod(dto.getPeriodo());
        entity.setStartDate(dto.getFechainicio());
        entity.setEndDate(dto.getFechafin());
        entity.setState(dto.getEstado());
        return toDTO(repository.save(entity));
    }

    @Override
    public void deleteById(Integer id) { repository.deleteById(id); }

    private PeriodDTO toDTO(Period e) {
        PeriodDTO dto = new PeriodDTO();
        dto.setIdperiodo(e.getPeriodId());
        dto.setPeriodo(e.getPeriod());
        dto.setFechainicio(e.getStartDate());
        dto.setFechafin(e.getEndDate());
        dto.setEstado(e.getState());
        return dto;
    }

    @Override
    public List<String> Period(List<PeriodDTO> dtos) {
        throw new UnsupportedOperationException("Unimplemented method 'Period'");
    }

    @Override
    public List<String> uploadPeriods(List<PeriodLoadDTO> periodDTOs) {
        throw new UnsupportedOperationException("Unimplemented method 'uploadPeriods'");
    }

    @Override
    @Transactional
    public SpResponseDTO createPeriod (PeriodCUDDTO periodCUD){
        try {
            return periodCustomRepo.createPeriod(periodCUD);
        } catch (Exception e) {
            return new SpResponseDTO("Error inesperado al crear el periodo académico", false);
        }
    }   

    @Override
    public SpResponseDTO updatePeriod(PeriodCUDDTO periodCUD){
        try {
            return periodCustomRepo.createPeriod(periodCUD);
        } catch (Exception e) {
            return new SpResponseDTO("Error inesperado al actualizar el periodo académico", false);
        }
    }
}
