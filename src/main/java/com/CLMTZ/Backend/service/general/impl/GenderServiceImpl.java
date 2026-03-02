package com.CLMTZ.Backend.service.general.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.CLMTZ.Backend.dto.general.GenderDTO;
import com.CLMTZ.Backend.model.general.Gender;
import com.CLMTZ.Backend.repository.general.IGenderRepository;
import com.CLMTZ.Backend.service.general.IGenderService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GenderServiceImpl implements IGenderService {

    private final IGenderRepository repository;

    @Override
    public List<GenderDTO> findAll() { return repository.findAll().stream().map(this::toDTO).collect(Collectors.toList()); }

    @Override
    public GenderDTO findById(Integer id) { return repository.findById(id).map(this::toDTO).orElseThrow(() -> new RuntimeException("Gender not found with id: " + id)); }

    @Override
    public GenderDTO save(GenderDTO dto) {
        Gender e = new Gender(); e.setGenderName(dto.getGenderName()); e.setState(dto.getState() != null ? dto.getState() : true);
        return toDTO(repository.save(e));
    }

    @Override
    public GenderDTO update(Integer id, GenderDTO dto) {
        Gender e = repository.findById(id).orElseThrow(() -> new RuntimeException("Gender not found with id: " + id));
        e.setGenderName(dto.getGenderName()); e.setState(dto.getState());
        return toDTO(repository.save(e));
    }

    @Override
    public void deleteById(Integer id) { repository.deleteById(id); }

    private GenderDTO toDTO(Gender e) { GenderDTO d = new GenderDTO(); d.setGenderId(e.getGenderId()); d.setGenderName(e.getGenderName()); d.setState(e.getState()); return d; }
}
