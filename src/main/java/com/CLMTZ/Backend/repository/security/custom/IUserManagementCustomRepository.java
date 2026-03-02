package com.CLMTZ.Backend.repository.security.custom;

import java.time.LocalDate;
import java.util.List;

import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.UserListManagementResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.UserRoleManagementResponseDTO;

public interface IUserManagementCustomRepository {

        List<UserListManagementResponseDTO> listUsersManagement(String filterUser, LocalDate date, Boolean state);

        SpResponseDTO createUserManagement(String user, String password, String roles);

        SpResponseDTO updateUserManagement(String jsonUserId);

        UserRoleManagementResponseDTO DataUserById(Integer idUser);
}
