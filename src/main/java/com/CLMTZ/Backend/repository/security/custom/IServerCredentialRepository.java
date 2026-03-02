package com.CLMTZ.Backend.repository.security.custom;

import java.util.Optional;

import com.CLMTZ.Backend.dto.security.Request.ServerCredentialRequestDTO;

public interface IServerCredentialRepository {
    Optional<ServerCredentialRequestDTO> getServerCredential(Integer userId, String masterKey);
}
