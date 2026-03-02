package com.CLMTZ.Backend.service.security.impl;

import com.CLMTZ.Backend.config.UserConnectionPool;
import com.CLMTZ.Backend.dto.security.Request.LoginRequestDTO;
import com.CLMTZ.Backend.dto.security.Request.ServerCredentialRequestDTO;
import com.CLMTZ.Backend.dto.security.Response.LoginResponseDTO;
import com.CLMTZ.Backend.dto.security.session.UserContext;
import com.CLMTZ.Backend.model.general.User;
import com.CLMTZ.Backend.model.security.Access;
import com.CLMTZ.Backend.model.security.UsersRoles;
import com.CLMTZ.Backend.repository.security.IAccessRepository;
import com.CLMTZ.Backend.repository.security.IUsersRolesRepository;
import com.CLMTZ.Backend.repository.security.custom.IServerCredentialRepository;
import com.CLMTZ.Backend.service.security.IAuthService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final String SESSION_CTX_KEY = "CTX";

    private final IAccessRepository accessRepository;
    private final IUsersRolesRepository usersRolesRepository;
    private final IServerCredentialRepository serverCredentialRepository;
    private final UserConnectionPool userConnectionPool;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${sgra.master-key}")
    private String masterKey;

    @Override
    public LoginResponseDTO login(LoginRequestDTO request, HttpSession session) {
        log.info("Intento de login para usuario: {}", request.getUsername());

        // 1. Buscar acceso por username
        Optional<Access> accessOpt = accessRepository.findByUsername(request.getUsername());
        if (accessOpt.isEmpty()) {
            log.warn("Usuario no encontrado: {}", request.getUsername());
            throw new RuntimeException("Credenciales inválidas");
        }

        Access access = accessOpt.get();

        // 2. Verificar el estado de la cuenta ('A' = Activo, 'I' = Inactivo, 'C' = Cambio contraseña)
        Character accountState = access.getState();
        if (accountState == null || accountState.equals('I')) {
            log.warn("Cuenta inactiva para usuario: {}", request.getUsername());
            throw new RuntimeException("La cuenta está desactivada. Contacte al administrador.");
        }

        // 3. Verificar password con BCrypt
        if (!passwordEncoder.matches(request.getPassword(), access.getPassword())) {
            log.warn("Contraseña incorrecta para usuario: {}", request.getUsername());
            throw new RuntimeException("Credenciales inválidas");
        }

        // 4. Obtener datos del usuario
        User user = access.getUser();
        if (user == null) {
            log.error("Usuario asociado no encontrado para acceso: {}", access.getAccessId());
            throw new RuntimeException("Error en la configuración del usuario");
        }

        // 5. Obtener roles del usuario
        List<UsersRoles> userRoles = usersRolesRepository.findActiveRolesByUserId(user.getUserId());
        List<String> roles = userRoles.stream()
                .map(ur -> ur.getRoleId().getRole())
                .collect(Collectors.toList());

        if (roles.isEmpty()) {
            log.warn("Usuario sin roles asignados: {}", request.getUsername());
            throw new RuntimeException("El usuario no tiene roles asignados");
        }

        // 6. Obtener credenciales del servidor (LOGIN SERVER)
        boolean serverSynced = false;
        String dbUser = null;
        String dbPassword = null;

        if (masterKey != null && !masterKey.isEmpty()) {
            Optional<ServerCredentialRequestDTO> serverCredOpt = serverCredentialRepository.getServerCredential(user.getUserId(), masterKey);
            if (serverCredOpt.isPresent()) {
                ServerCredentialRequestDTO serverCred = serverCredOpt.get();
                serverSynced = true;
                dbUser = serverCred.getDbUser();
                dbPassword = serverCred.getDbPassword();
                log.info("Credenciales de servidor sincronizadas para usuario: {}", request.getUsername());
            } else {
                log.warn("Credenciales de servidor no sincronizadas para usuario: {}", request.getUsername());
                throw new RuntimeException("Credenciales de servidor no sincronizadas. Contacte al administrador.");
            }
        } else {
            log.warn("Master key no configurada. Las credenciales de servidor no pueden ser verificadas.");
            throw new RuntimeException("Error de configuración del sistema");
        }

        // 7. Crear UserContext y guardar en sesión
        UserContext ctx = new UserContext();
        ctx.setUserId(user.getUserId());
        ctx.setUsername(access.getUsername());
        ctx.setFirstName(user.getFirstName());
        ctx.setLastName(user.getLastName());
        ctx.setEmail(user.getEmail());
        ctx.setRoles(roles);
        ctx.setServerSynced(serverSynced);
        ctx.setAccountState(accountState);
        ctx.setDbUser(dbUser);
        ctx.setDbPassword(dbPassword); // Solo en memoria de sesión

        session.setAttribute(SESSION_CTX_KEY, ctx);
        log.info("Login exitoso para usuario: {}. Roles: {}", request.getUsername(), roles);

        // 8. Retornar respuesta (sin dbPassword)
        return new LoginResponseDTO(
                user.getUserId(),
                access.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                roles,
                serverSynced,
                accountState
        );
    }

    @Override
    public LoginResponseDTO getCurrentUser(HttpSession session) {
        UserContext ctx = getUserContext(session);
        if (ctx == null) {
            return null;
        }
        return new LoginResponseDTO(
                ctx.getUserId(),
                ctx.getUsername(),
                ctx.getFirstName(),
                ctx.getLastName(),
                ctx.getEmail(),
                ctx.getRoles(),
                ctx.isServerSynced(),
                ctx.getAccountState()
        );
    }

    @Override
    public void logout(HttpSession session) {
        UserContext ctx = getUserContext(session);
        if (ctx != null) {
            log.info("Logout para usuario: {}", ctx.getUsername());
            // Liberar pool de conexiones del usuario
            if (ctx.getDbUser() != null) {
                userConnectionPool.evict(ctx.getDbUser());
            }
        }
        session.invalidate();
    }

    @Override
    public UserContext getUserContext(HttpSession session) {
        Object ctx = session.getAttribute(SESSION_CTX_KEY);
        if (ctx instanceof UserContext) {
            return (UserContext) ctx;
        }
        return null;
    }
}

