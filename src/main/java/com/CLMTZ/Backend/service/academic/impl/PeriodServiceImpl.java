package com.CLMTZ.Backend.service.academic.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.CLMTZ.Backend.dto.academic.PeriodDTO;
import com.CLMTZ.Backend.dto.academic.PeriodLoadDTO;
import com.CLMTZ.Backend.model.academic.Period;
import com.CLMTZ.Backend.repository.academic.IPeriodRepository;
import com.CLMTZ.Backend.service.academic.IPeriodService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PeriodServiceImpl implements IPeriodService {

    private final IPeriodRepository repository;

    @Override
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
        entity.setPeriod(dto.getPeriod());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setState(dto.getState() != null ? dto.getState() : true);
        return toDTO(repository.save(entity));
    }

    @Override
    public PeriodDTO update(Integer id, PeriodDTO dto) {
        Period entity = repository.findById(id).orElseThrow(() -> new RuntimeException("Period not found with id: " + id));
        entity.setPeriod(dto.getPeriod());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setState(dto.getState());
        return toDTO(repository.save(entity));
    }

    @Override
    public void deleteById(Integer id) { repository.deleteById(id); }

    private PeriodDTO toDTO(Period e) {
        PeriodDTO dto = new PeriodDTO();
        dto.setPeriodId(e.getPeriodId());
        dto.setPeriod(e.getPeriod());
        dto.setStartDate(e.getStartDate());
        dto.setEndDate(e.getEndDate());
        dto.setState(e.getState());
        return dto;
    }

    @Override
    public List<String> Period(List<PeriodDTO> dtos) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'Period'");
    }

    @Override
    public List<String> uploadPeriods(List<PeriodLoadDTO> periodDTOs) {
        List<String> report = new java.util.ArrayList<>();
        for (PeriodLoadDTO dto : periodDTOs) {
            try {
                Period period = repository.findAll().stream()
                    .filter(p -> p.getPeriod().equalsIgnoreCase(dto.getNombrePeriodo()))
                    .findFirst().orElse(null);
                if (period == null) {
                    period = new Period();
                    period.setPeriod(dto.getNombrePeriodo());
                    period.setStartDate(dto.getFechaInicio());
                    period.setEndDate(dto.getFechaFin());
                    period.setState(true);
                    repository.save(period);
                    report.add("Periodo '" + dto.getNombrePeriodo() + "' creado");
                } else {
                    period.setStartDate(dto.getFechaInicio());
                    period.setEndDate(dto.getFechaFin());
                    repository.save(period);
                    report.add("Periodo '" + dto.getNombrePeriodo() + "' actualizado");
                }
            } catch (Exception e) {
                report.add("Periodo '" + dto.getNombrePeriodo() + "': ERROR (" + e.getCause().getMessage() + ")");
            }
        }
        return report;
    }

    
}
