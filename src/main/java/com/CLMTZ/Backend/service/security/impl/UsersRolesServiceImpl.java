package com.CLMTZ.Backend.service.security.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

import com.CLMTZ.Backend.dto.security.Request.UsersRolesRequestDTO;
import com.CLMTZ.Backend.model.security.UsersRoles;
import com.CLMTZ.Backend.repository.security.IUsersRolesRepository;
import com.CLMTZ.Backend.repository.security.IRoleRepository;
import com.CLMTZ.Backend.repository.general.IUserRepository;
import com.CLMTZ.Backend.service.security.IUsersRolesService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsersRolesServiceImpl implements IUsersRolesService {
    
}
