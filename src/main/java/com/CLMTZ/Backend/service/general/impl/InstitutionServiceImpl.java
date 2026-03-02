package com.CLMTZ.Backend.service.general.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.CLMTZ.Backend.dto.general.InstitutionDTO;
import com.CLMTZ.Backend.model.general.Institution;
import com.CLMTZ.Backend.repository.general.IInstitutionRepository;
import com.CLMTZ.Backend.service.general.IInstitutionService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InstitutionServiceImpl implements IInstitutionService {

    private final IInstitutionRepository repository;

    @Override
    public List<InstitutionDTO> findAll() { return repository.findAll().stream().map(this::toDTO).collect(Collectors.toList()); }

    @Override
    public InstitutionDTO findById(Integer id) { return repository.findById(id).map(this::toDTO).orElseThrow(() -> new RuntimeException("Institution not found with id: " + id)); }

    @Override
    public InstitutionDTO save(InstitutionDTO dto) {
        Institution e = new Institution(); e.setNameInstitution(dto.getNameInstitution()); e.setState(dto.getState() != null ? dto.getState() : true);
        return toDTO(repository.save(e));
    }

    @Override
    public InstitutionDTO update(Integer id, InstitutionDTO dto) {
        Institution e = repository.findById(id).orElseThrow(() -> new RuntimeException("Institution not found with id: " + id));
        e.setNameInstitution(dto.getNameInstitution()); e.setState(dto.getState());
        return toDTO(repository.save(e));
    }

    @Override
    public void deleteById(Integer id) { repository.deleteById(id); }

    private InstitutionDTO toDTO(Institution e) { InstitutionDTO d = new InstitutionDTO(); d.setInstitutionId(e.getInstitutionId()); d.setNameInstitution(e.getNameInstitution()); d.setState(e.getState()); return d; }
}
