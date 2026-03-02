package com.CLMTZ.Backend.repository.security.custom.impl;

import com.CLMTZ.Backend.dto.security.Request.ServerCredentialRequestDTO;
import com.CLMTZ.Backend.repository.security.custom.IServerCredentialRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ServerCredentialRepositoryImpl implements IServerCredentialRepository {

    private static final Logger log = LoggerFactory.getLogger(ServerCredentialRepositoryImpl.class);

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public ServerCredentialRepositoryImpl(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @Override
    public Optional<ServerCredentialRequestDTO> getServerCredential(Integer userId, String masterKey) {
        try {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("userId", userId);
            params.addValue("masterKey", masterKey);

            List<ServerCredentialRequestDTO> results = namedParameterJdbcTemplate.query(
                    getServerCredentialQuery(),
                    params,
                    (rs, rowNum) -> {
                        ServerCredentialRequestDTO credential = new ServerCredentialRequestDTO();
                        credential.setDbUser(rs.getString("db_usuario"));
                        credential.setDbPassword(rs.getString("db_password"));
                        return credential;
                    });

            if (results.isEmpty()) {
                log.warn("No se encontraron credenciales de servidor para userId: {}", userId);
                return Optional.empty();
            }

            log.info("Credenciales de servidor obtenidas correctamente para userId: {}", userId);
            return Optional.of(results.getFirst());

        } catch (Exception e) {
            log.error("Error al obtener credenciales de servidor para userId: {}", userId, e);
            return Optional.empty();
        }
    }

    private String getServerCredentialQuery() {
        return "SELECT * FROM seguridad.fn_get_server_credential(:userId, :masterKey)";
    }
}