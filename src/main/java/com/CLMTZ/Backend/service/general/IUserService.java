package com.CLMTZ.Backend.service.general;

import java.util.List;

import com.CLMTZ.Backend.dto.general.UserDTO;

public interface IUserService {
    List<UserDTO> findAll();
    UserDTO findById(Integer id);
    UserDTO save(UserDTO dto);
    UserDTO update(Integer id, UserDTO dto);
    void deleteById(Integer id);
}
