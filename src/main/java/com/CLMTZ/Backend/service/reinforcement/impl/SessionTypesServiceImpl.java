package com.CLMTZ.Backend.service.reinforcement.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.CLMTZ.Backend.dto.reinforcement.SessionTypesDTO;
import com.CLMTZ.Backend.model.reinforcement.SessionTypes;
import com.CLMTZ.Backend.repository.reinforcement.ISessionTypesRepository;
import com.CLMTZ.Backend.service.reinforcement.ISessionTypesService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SessionTypesServiceImpl implements ISessionTypesService {

    private final ISessionTypesRepository repository;

    @Override
    public List<SessionTypesDTO> findAll() { return repository.findAll().stream().map(this::toDTO).collect(Collectors.toList()); }

    @Override
    public SessionTypesDTO findById(Integer id) { return repository.findById(id).map(this::toDTO).orElseThrow(() -> new RuntimeException("SessionTypes not found with id: " + id)); }

    @Override
    public SessionTypesDTO save(SessionTypesDTO dto) {
        SessionTypes e = new SessionTypes();
        e.setSesionType(dto.getSesionType()); e.setState(dto.getState() != null ? dto.getState() : true);
        return toDTO(repository.save(e));
    }

    @Override
    public SessionTypesDTO update(Integer id, SessionTypesDTO dto) {
        SessionTypes e = repository.findById(id).orElseThrow(() -> new RuntimeException("SessionTypes not found with id: " + id));
        e.setSesionType(dto.getSesionType()); e.setState(dto.getState());
        return toDTO(repository.save(e));
    }

    @Override
    public void deleteById(Integer id) { repository.deleteById(id); }

    private SessionTypesDTO toDTO(SessionTypes e) { SessionTypesDTO d = new SessionTypesDTO(); d.setSesionTypesId(e.getSesionTypesId()); d.setSesionType(e.getSesionType()); d.setState(e.getState()); return d; }
}
