package com.CLMTZ.Backend.service.general.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.CLMTZ.Backend.dto.general.UserDTO;
import com.CLMTZ.Backend.model.general.User;
import com.CLMTZ.Backend.repository.general.IUserRepository;
import com.CLMTZ.Backend.repository.general.IInstitutionRepository;
import com.CLMTZ.Backend.repository.general.IGenderRepository;
import com.CLMTZ.Backend.service.general.IUserService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final IUserRepository repository;
    private final IInstitutionRepository institutionRepository;
    private final IGenderRepository genderRepository;

    @Override
    public List<UserDTO> findAll() { return repository.findAll().stream().map(this::toDTO).collect(Collectors.toList()); }

    @Override
    public UserDTO findById(Integer id) { return repository.findById(id).map(this::toDTO).orElseThrow(() -> new RuntimeException("User not found with id: " + id)); }

    @Override
    public UserDTO save(UserDTO dto) {
        User e = new User();
        e.setFirstName(dto.getFirstName()); e.setLastName(dto.getLastName()); e.setIdentification(dto.getIdentification());
        e.setPhoneNumber(dto.getPhoneNumber()); e.setEmail(dto.getEmail()); e.setAddress(dto.getAddress());
        if (dto.getInstitutionId() != null) e.setInstitutionId(institutionRepository.findById(dto.getInstitutionId()).orElseThrow(() -> new RuntimeException("Institution not found")));
        if (dto.getGenderId() != null) e.setIdGender(genderRepository.findById(dto.getGenderId()).orElseThrow(() -> new RuntimeException("Gender not found")));
        return toDTO(repository.save(e));
    }

    @Override
    public UserDTO update(Integer id, UserDTO dto) {
        User e = repository.findById(id).orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        e.setFirstName(dto.getFirstName()); e.setLastName(dto.getLastName()); e.setIdentification(dto.getIdentification());
        e.setPhoneNumber(dto.getPhoneNumber()); e.setEmail(dto.getEmail()); e.setAddress(dto.getAddress());
        if (dto.getInstitutionId() != null) e.setInstitutionId(institutionRepository.findById(dto.getInstitutionId()).orElseThrow(() -> new RuntimeException("Institution not found")));
        if (dto.getGenderId() != null) e.setIdGender(genderRepository.findById(dto.getGenderId()).orElseThrow(() -> new RuntimeException("Gender not found")));
        return toDTO(repository.save(e));
    }

    @Override
    public void deleteById(Integer id) { repository.deleteById(id); }

    private UserDTO toDTO(User e) {
        UserDTO d = new UserDTO();
        d.setUserId(e.getUserId()); d.setFirstName(e.getFirstName()); d.setLastName(e.getLastName());
        d.setIdentification(e.getIdentification()); d.setPhoneNumber(e.getPhoneNumber()); d.setEmail(e.getEmail()); d.setAddress(e.getAddress());
        d.setInstitutionId(e.getInstitutionId() != null ? e.getInstitutionId().getInstitutionId() : null);
        d.setGenderId(e.getIdGender() != null ? e.getIdGender().getGenderId() : null);
        return d;
    }
}
