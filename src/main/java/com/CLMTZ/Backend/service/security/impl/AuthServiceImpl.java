package com.CLMTZ.Backend.service.security.impl;

import com.CLMTZ.Backend.config.UserConnectionPool;
import com.CLMTZ.Backend.dto.security.Request.ChangePasswordRequestDTO;
import com.CLMTZ.Backend.dto.security.Request.LoginRequestDTO;
import com.CLMTZ.Backend.dto.security.Request.ServerCredentialRequestDTO;
import com.CLMTZ.Backend.dto.security.Request.VoluntaryChangePasswordRequestDTO;
import com.CLMTZ.Backend.dto.security.Response.LoginResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;
import com.CLMTZ.Backend.dto.security.session.UserContext;
import com.CLMTZ.Backend.model.general.User;
import com.CLMTZ.Backend.model.security.Access;
import com.CLMTZ.Backend.model.security.UsersRoles;
import com.CLMTZ.Backend.repository.security.custom.ICredentialRepository;
import com.CLMTZ.Backend.repository.security.custom.IServerCredentialRepository;
import com.CLMTZ.Backend.repository.security.jpa.IAccessRepository;
import com.CLMTZ.Backend.repository.security.jpa.IUsersRolesRepository;
import com.CLMTZ.Backend.service.security.IAccessAuditService;
import com.CLMTZ.Backend.service.security.IAuthService;

import jakarta.servlet.http.HttpServletRequest;
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
    private final ICredentialRepository credentialRepository;
    private final UserConnectionPool userConnectionPool;
    private final IAccessAuditService accessAuditSer;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${sgra-master-key}")
    private String masterKey;

    @Override
    public LoginResponseDTO login(LoginRequestDTO request, HttpSession session, HttpServletRequest requestSer) {
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

        // 6. Si el estado es 'C' (Cambiar contraseña), NO buscar credenciales de servidor
        //    porque aún no existen. Se crearán al momento del primer cambio de contraseña.
        boolean serverSynced = false;
        String dbUser = null;
        String dbPassword = null;

        if (accountState.equals('C')) {
            log.info("Usuario con estado 'C' (cambio de contraseña requerido): {}. " +
                    "Se omite la obtención de credenciales de servidor.", request.getUsername());
            serverSynced = false;
        } else {
            // Estado 'A' → obtener credenciales del servidor (LOGIN SERVER)
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
        }

        session.invalidate();
        HttpSession newSession = requestSer.getSession(true);
        log.debug("Sesión regenerada. Nuevo ID: {}", newSession.getId());

        // 8. Crear UserContext y guardar en la NUEVA sesión
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

        Integer auditId = accessAuditSer.auditAccess(requestSer, user.getUserId(), "Acceso", newSession.getId());

        ctx.setIdAuditoriaAcceso(auditId);

        newSession.setAttribute(SESSION_CTX_KEY, ctx);
        log.info("Login exitoso para usuario: {}. Roles: {}. Estado: {}", request.getUsername(), roles, accountState);

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
    public void logout(HttpSession session, HttpServletRequest requestSer) {
        UserContext ctx = getUserContext(session);
        if (ctx != null) {
            accessAuditSer.auditAccess(requestSer, ctx.getUserId(), "Cierre sesion", session.getId());
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

    @Override
    public SpResponseDTO changePassword(ChangePasswordRequestDTO request, HttpSession session) {
        // 1. Verificar sesión activa
        UserContext ctx = getUserContext(session);
        if (ctx == null) {
            return new SpResponseDTO("No hay sesión activa.", false);
        }

        // 2. Verificar que el usuario esté en estado 'C' (Cambiar contraseña)
        if (ctx.getAccountState() == null || !ctx.getAccountState().equals('C')) {
            return new SpResponseDTO("Esta acción solo está disponible para usuarios que requieren cambio de contraseña.", false);
        }

        // 3. Validar que las contraseñas no estén vacías
        if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
            return new SpResponseDTO("La nueva contraseña no puede estar vacía.", false);
        }

        if (request.getConfirmPassword() == null || request.getConfirmPassword().trim().isEmpty()) {
            return new SpResponseDTO("La confirmación de contraseña no puede estar vacía.", false);
        }

        // 4. Validar que coincidan
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return new SpResponseDTO("Las contraseñas no coinciden.", false);
        }

        // 5. Validar complejidad mínima (al menos 8 caracteres, 1 mayúscula, 1 número)
        String password = request.getNewPassword();
        if (password.length() < 8) {
            return new SpResponseDTO("La contraseña debe tener al menos 8 caracteres.", false);
        }
        if (!password.matches(".*[A-Z].*")) {
            return new SpResponseDTO("La contraseña debe contener al menos una letra mayúscula.", false);
        }
        if (!password.matches(".*[0-9].*")) {
            return new SpResponseDTO("La contraseña debe contener al menos un número.", false);
        }

        // 6. Llamar al SP que hace todo el pipeline:
        //    - Hashea la nueva contraseña y actualiza tbaccesos (estado='A')
        //    - Crea el usuario PostgreSQL SERVER
        //    - Vincula en tbusuariosgestionusuarios
        log.info("Procesando primer cambio de contraseña para usuario: {} (userId={})",
                ctx.getUsername(), ctx.getUserId());

        SpResponseDTO result = credentialRepository.firstPasswordChange(ctx.getUserId(), password);

        if (Boolean.TRUE.equals(result.getSuccess())) {
            log.info("Cambio de contraseña exitoso para usuario: {}. Invalidando sesión.", ctx.getUsername());
            // Invalidar sesión para forzar nuevo login con credenciales actualizadas
            session.invalidate();
        } else {
            log.warn("Error en cambio de contraseña para usuario: {}. Mensaje: {}",
                    ctx.getUsername(), result.getMessage());
        }

        return result;
    }

    @Override
    public SpResponseDTO voluntaryChangePassword(VoluntaryChangePasswordRequestDTO request, HttpSession session) {
        // 1. Verificar sesión activa
        UserContext ctx = getUserContext(session);
        if (ctx == null) {
            return new SpResponseDTO("No hay sesión activa.", false);
        }

        // 2. Verificar que el usuario esté en estado 'A' (Activo)
        if (ctx.getAccountState() == null || !ctx.getAccountState().equals('A')) {
            return new SpResponseDTO("Esta acción solo está disponible para usuarios activos.", false);
        }

        // 3. Validar que los campos no estén vacíos
        if (request.getCurrentPassword() == null || request.getCurrentPassword().trim().isEmpty()) {
            return new SpResponseDTO("La contraseña actual no puede estar vacía.", false);
        }

        if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
            return new SpResponseDTO("La nueva contraseña no puede estar vacía.", false);
        }

        if (request.getConfirmPassword() == null || request.getConfirmPassword().trim().isEmpty()) {
            return new SpResponseDTO("La confirmación de contraseña no puede estar vacía.", false);
        }

        // 4. Validar que coincidan
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return new SpResponseDTO("Las contraseñas no coinciden.", false);
        }

        // 5. Validar complejidad mínima (al menos 8 caracteres, 1 mayúscula, 1 número)
        String password = request.getNewPassword();
        if (password.length() < 8) {
            return new SpResponseDTO("La contraseña debe tener al menos 8 caracteres.", false);
        }
        if (!password.matches(".*[A-Z].*")) {
            return new SpResponseDTO("La contraseña debe contener al menos una letra mayúscula.", false);
        }
        if (!password.matches(".*[0-9].*")) {
            return new SpResponseDTO("La contraseña debe contener al menos un número.", false);
        }

        // 6. Llamar al SP de cambio voluntario de contraseña
        log.info("Procesando cambio voluntario de contraseña para usuario: {} (userId={})",
                ctx.getUsername(), ctx.getUserId());

        SpResponseDTO result = credentialRepository.voluntaryPasswordChange(
                ctx.getUserId(), request.getCurrentPassword(), password);

        if (Boolean.TRUE.equals(result.getSuccess())) {
            log.info("Cambio voluntario de contraseña exitoso para usuario: {}", ctx.getUsername());
        } else {
            log.warn("Error en cambio voluntario de contraseña para usuario: {}. Mensaje: {}",
                    ctx.getUsername(), result.getMessage());
        }

        return result;
    }
}

