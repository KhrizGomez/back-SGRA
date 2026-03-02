package com.CLMTZ.Backend.service.reinforcement.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.CLMTZ.Backend.dto.reinforcement.WorkAreaManagerDTO;
import com.CLMTZ.Backend.model.academic.AcademicArea;
import com.CLMTZ.Backend.model.general.User;
import com.CLMTZ.Backend.model.reinforcement.WorkAreaManager;
import com.CLMTZ.Backend.repository.academic.IAcademicAreaRepository;
import com.CLMTZ.Backend.repository.general.IUserRepository;
import com.CLMTZ.Backend.repository.reinforcement.IWorkAreaManagerRepository;
import com.CLMTZ.Backend.service.reinforcement.IWorkAreaManagerService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkAreaManagerServiceImpl implements IWorkAreaManagerService {

    private final IWorkAreaManagerRepository repository;
    private final IUserRepository userRepository;
    private final IAcademicAreaRepository academicAreaRepository;

    @Override
    public List<WorkAreaManagerDTO> findAll() {
        return repository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public WorkAreaManagerDTO findById(Integer id) {
        return repository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("WorkAreaManager not found with id: " + id));
    }

    @Override
    public WorkAreaManagerDTO save(WorkAreaManagerDTO dto) {
        WorkAreaManager e = new WorkAreaManager();
        if (dto.getUserId() != null) {
            User user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + dto.getUserId()));
            e.setUserId(user);
        }
        if (dto.getAreaAcademicId() != null) {
            AcademicArea area = academicAreaRepository.findById(dto.getAreaAcademicId())
                    .orElseThrow(() -> new RuntimeException("AcademicArea not found with id: " + dto.getAreaAcademicId()));
            e.setAreaAcademicId(area);
        }
        e.setPlant(dto.getPlant());
        e.setState(dto.getState() != null ? dto.getState() : true);
        return toDTO(repository.save(e));
    }

    @Override
    public WorkAreaManagerDTO update(Integer id, WorkAreaManagerDTO dto) {
        WorkAreaManager e = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("WorkAreaManager not found with id: " + id));
        if (dto.getUserId() != null) {
            User user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + dto.getUserId()));
            e.setUserId(user);
        }
        if (dto.getAreaAcademicId() != null) {
            AcademicArea area = academicAreaRepository.findById(dto.getAreaAcademicId())
                    .orElseThrow(() -> new RuntimeException("AcademicArea not found with id: " + dto.getAreaAcademicId()));
            e.setAreaAcademicId(area);
        }
        e.setPlant(dto.getPlant());
        e.setState(dto.getState());
        return toDTO(repository.save(e));
    }

    @Override
    public void deleteById(Integer id) {
        repository.deleteById(id);
    }

    private WorkAreaManagerDTO toDTO(WorkAreaManager e) {
        WorkAreaManagerDTO d = new WorkAreaManagerDTO();
        d.setWorkAreaManagerId(e.getWorkAreaManagerId());
        d.setUserId(e.getUserId() != null ? e.getUserId().getUserId() : null);
        d.setAreaAcademicId(e.getAreaAcademicId() != null ? e.getAreaAcademicId().getAreaAcademicId() : null);
        d.setPlant(e.getPlant());
        d.setState(e.getState());
        return d;
    }
}
