package com.CLMTZ.Backend.service.reinforcement.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.CLMTZ.Backend.dto.reinforcement.ParticipantsDTO;
import com.CLMTZ.Backend.model.reinforcement.Participants;
import com.CLMTZ.Backend.repository.reinforcement.IParticipantsRepository;
import com.CLMTZ.Backend.repository.reinforcement.IReinforcementRequestRepository;
import com.CLMTZ.Backend.repository.academic.IStudentsRepository;
import com.CLMTZ.Backend.service.reinforcement.IParticipantsService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ParticipantsServiceImpl implements IParticipantsService {

    private final IParticipantsRepository repository;
    private final IStudentsRepository studentsRepository;
    private final IReinforcementRequestRepository reinforcementRequestRepository;

    @Override
    public List<ParticipantsDTO> findAll() { return repository.findAll().stream().map(this::toDTO).collect(Collectors.toList()); }

    @Override
    public ParticipantsDTO findById(Integer id) { return repository.findById(id).map(this::toDTO).orElseThrow(() -> new RuntimeException("Participants not found with id: " + id)); }

    @Override
    public ParticipantsDTO save(ParticipantsDTO dto) {
        Participants e = new Participants();
        e.setStake(dto.getStake());
        if (dto.getStudentId() != null) e.setStudentId(studentsRepository.findById(dto.getStudentId()).orElseThrow(() -> new RuntimeException("Student not found")));
        if (dto.getReinforcementRequestId() != null) e.setReinforcementRequestId(reinforcementRequestRepository.findById(dto.getReinforcementRequestId()).orElseThrow(() -> new RuntimeException("ReinforcementRequest not found")));
        return toDTO(repository.save(e));
    }

    @Override
    public ParticipantsDTO update(Integer id, ParticipantsDTO dto) {
        Participants e = repository.findById(id).orElseThrow(() -> new RuntimeException("Participants not found with id: " + id));
        e.setStake(dto.getStake());
        if (dto.getStudentId() != null) e.setStudentId(studentsRepository.findById(dto.getStudentId()).orElseThrow(() -> new RuntimeException("Student not found")));
        if (dto.getReinforcementRequestId() != null) e.setReinforcementRequestId(reinforcementRequestRepository.findById(dto.getReinforcementRequestId()).orElseThrow(() -> new RuntimeException("ReinforcementRequest not found")));
        return toDTO(repository.save(e));
    }

    @Override
    public void deleteById(Integer id) { repository.deleteById(id); }

    private ParticipantsDTO toDTO(Participants e) {
        ParticipantsDTO d = new ParticipantsDTO();
        d.setParticipantId(e.getParticipantId()); d.setStake(e.getStake());
        d.setStudentId(e.getStudentId() != null ? e.getStudentId().getStudentId() : null);
        d.setReinforcementRequestId(e.getReinforcementRequestId() != null ? e.getReinforcementRequestId().getReinforcementRequestId() : null);
        return d;
    }
}
