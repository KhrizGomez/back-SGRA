package com.CLMTZ.Backend.service.security.impl;

import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;
import com.CLMTZ.Backend.model.general.User;
import com.CLMTZ.Backend.model.security.Access;
import com.CLMTZ.Backend.repository.general.IUserRepository;
import com.CLMTZ.Backend.repository.security.custom.ICredentialRepository;
import com.CLMTZ.Backend.repository.security.jpa.IAccessRepository;
import com.CLMTZ.Backend.service.external.IEmailService;
import com.CLMTZ.Backend.service.security.IPasswordRecoveryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PasswordRecoveryServiceImpl implements IPasswordRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(PasswordRecoveryServiceImpl.class);

    private final IUserRepository userRepository;
    private final IAccessRepository accessRepository;
    private final ICredentialRepository credentialRepository;
    private final IEmailService emailService;

    private static final int CODE_LENGTH = 6;
    private static final int CODE_EXPIRATION_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;
    private static final int COOLDOWN_SECONDS = 60;

    // Almacenamiento en memoria de tokens de recuperación
    private final ConcurrentHashMap<String, RecoveryToken> recoveryTokens = new ConcurrentHashMap<>();

    /**
     * Estructura interna para almacenar el token de recuperación.
     */
    private static class RecoveryToken {
        final String code;
        final Integer userId;
        final LocalDateTime expiresAt;
        final LocalDateTime createdAt;
        int attempts;
        boolean verified;

        RecoveryToken(String code, Integer userId) {
            this.code = code;
            this.userId = userId;
            this.expiresAt = LocalDateTime.now().plusMinutes(CODE_EXPIRATION_MINUTES);
            this.createdAt = LocalDateTime.now();
            this.attempts = 0;
            this.verified = false;
        }

        boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }

        boolean isInCooldown() {
            return LocalDateTime.now().isBefore(createdAt.plusSeconds(COOLDOWN_SECONDS));
        }

        boolean hasExceededAttempts() {
            return attempts >= MAX_ATTEMPTS;
        }
    }

    @Override
    public SpResponseDTO requestRecoveryCode(String email) {
        if (email == null || email.trim().isEmpty()) {
            return new SpResponseDTO("El correo electrónico es obligatorio.", false);
        }

        String normalizedEmail = email.trim().toLowerCase();

        // Verificar cooldown: si ya existe un token reciente, no permitir reenvío
        RecoveryToken existingToken = recoveryTokens.get(normalizedEmail);
        if (existingToken != null && !existingToken.isExpired() && existingToken.isInCooldown()) {
            return new SpResponseDTO("Ya se envió un código recientemente. Espera un momento antes de solicitar otro.", false);
        }

        // Buscar usuario por email
        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);
        if (userOpt.isEmpty()) {
            // Por seguridad, no revelar si el correo existe o no
            log.warn("Intento de recuperación para correo no registrado: {}", normalizedEmail);
            return new SpResponseDTO("Si el correo está registrado, recibirás un código de verificación.", true);
        }

        User user = userOpt.get();

        // Verificar que el usuario tenga acceso activo
        Optional<Access> accessOpt = accessRepository.findByUsername(user.getAccess().getUsername());
        if (accessOpt.isEmpty()) {
            log.warn("Usuario sin credenciales de acceso: {}", normalizedEmail);
            return new SpResponseDTO("Si el correo está registrado, recibirás un código de verificación.", true);
        }

        Access access = accessOpt.get();
        Character state = access.getState();

        if (state == null || state.equals('I')) {
            log.warn("Intento de recuperación para cuenta inactiva: {}", normalizedEmail);
            return new SpResponseDTO("Si el correo está registrado, recibirás un código de verificación.", true);
        }

        if (state.equals('C')) {
            log.warn("Intento de recuperación para cuenta con cambio de contraseña pendiente: {}", normalizedEmail);
            return new SpResponseDTO("Tu cuenta aún no ha sido activada. Revisa tu correo con las credenciales temporales e inicia sesión por primera vez.", false);
        }

        // Generar código de 6 dígitos
        String code = generateCode();

        // Almacenar token
        RecoveryToken token = new RecoveryToken(code, user.getUserId());
        recoveryTokens.put(normalizedEmail, token);

        // Enviar correo con el código
        try {
            String htmlBody = buildRecoveryEmailBody(user.getFirstName(), code);
            emailService.sendEmail(normalizedEmail, "Código de recuperación de contraseña - SGRA", htmlBody);
            log.info("Código de recuperación enviado a: {}", normalizedEmail);
        } catch (Exception e) {
            log.error("Error al enviar correo de recuperación a {}: {}", normalizedEmail, e.getMessage());
            recoveryTokens.remove(normalizedEmail);
            return new SpResponseDTO("Error al enviar el correo. Intenta nuevamente más tarde.", false);
        }

        return new SpResponseDTO("Si el correo está registrado, recibirás un código de verificación.", true);
    }

    @Override
    public SpResponseDTO verifyRecoveryCode(String email, String code) {
        if (email == null || email.trim().isEmpty()) {
            return new SpResponseDTO("El correo electrónico es obligatorio.", false);
        }
        if (code == null || code.trim().isEmpty()) {
            return new SpResponseDTO("El código de verificación es obligatorio.", false);
        }

        String normalizedEmail = email.trim().toLowerCase();
        RecoveryToken token = recoveryTokens.get(normalizedEmail);

        if (token == null) {
            return new SpResponseDTO("No se encontró una solicitud de recuperación. Solicita un nuevo código.", false);
        }

        if (token.isExpired()) {
            recoveryTokens.remove(normalizedEmail);
            return new SpResponseDTO("El código ha expirado. Solicita uno nuevo.", false);
        }

        if (token.hasExceededAttempts()) {
            recoveryTokens.remove(normalizedEmail);
            return new SpResponseDTO("Se excedió el número máximo de intentos. Solicita un nuevo código.", false);
        }

        token.attempts++;

        if (!token.code.equals(code.trim())) {
            int remaining = MAX_ATTEMPTS - token.attempts;
            if (remaining <= 0) {
                recoveryTokens.remove(normalizedEmail);
                return new SpResponseDTO("Código incorrecto. Se agotaron los intentos. Solicita un nuevo código.", false);
            }
            return new SpResponseDTO("Código incorrecto. Te quedan " + remaining + " intento(s).", false);
        }

        // Código correcto: marcarlo como verificado
        token.verified = true;
        log.info("Código de recuperación verificado correctamente para: {}", normalizedEmail);

        return new SpResponseDTO("Código verificado correctamente.", true);
    }

    @Override
    public SpResponseDTO resetPassword(String email, String code, String newPassword, String confirmPassword) {
        if (email == null || email.trim().isEmpty()) {
            return new SpResponseDTO("El correo electrónico es obligatorio.", false);
        }

        String normalizedEmail = email.trim().toLowerCase();
        RecoveryToken token = recoveryTokens.get(normalizedEmail);

        // Verificar que exista un token verificado
        if (token == null) {
            return new SpResponseDTO("No se encontró una solicitud de recuperación válida.", false);
        }

        if (token.isExpired()) {
            recoveryTokens.remove(normalizedEmail);
            return new SpResponseDTO("La solicitud de recuperación ha expirado. Solicita un nuevo código.", false);
        }

        if (!token.verified) {
            return new SpResponseDTO("El código no ha sido verificado. Verifica tu código primero.", false);
        }

        // Validar que el código coincida (doble verificación)
        if (!token.code.equals(code.trim())) {
            return new SpResponseDTO("El código de verificación no es válido.", false);
        }

        // Validar contraseñas
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return new SpResponseDTO("La nueva contraseña no puede estar vacía.", false);
        }

        if (confirmPassword == null || confirmPassword.trim().isEmpty()) {
            return new SpResponseDTO("La confirmación de contraseña no puede estar vacía.", false);
        }

        if (!newPassword.equals(confirmPassword)) {
            return new SpResponseDTO("Las contraseñas no coinciden.", false);
        }

        // Validar complejidad mínima
        if (newPassword.length() < 8) {
            return new SpResponseDTO("La contraseña debe tener al menos 8 caracteres.", false);
        }
        if (!newPassword.matches(".*[A-Z].*")) {
            return new SpResponseDTO("La contraseña debe contener al menos una letra mayúscula.", false);
        }
        if (!newPassword.matches(".*[0-9].*")) {
            return new SpResponseDTO("La contraseña debe contener al menos un número.", false);
        }

        // Llamar al SP de recuperación de contraseña
        log.info("Ejecutando recuperación de contraseña para userId={}", token.userId);
        SpResponseDTO result = credentialRepository.recoverPassword(token.userId, newPassword);

        if (Boolean.TRUE.equals(result.getSuccess())) {
            // Limpiar token después de uso exitoso
            recoveryTokens.remove(normalizedEmail);
            log.info("Contraseña recuperada exitosamente para: {}", normalizedEmail);
        }

        return result;
    }

    // ─── Utilidades internas ───

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        int code = random.nextInt((int) Math.pow(10, CODE_LENGTH));
        return String.format("%0" + CODE_LENGTH + "d", code);
    }

    private String buildRecoveryEmailBody(String firstName, String code) {
        return """
                <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 500px; margin: 0 auto; padding: 20px;">
                  <div style="background: linear-gradient(135deg, #2e7d32, #1b5e20); padding: 24px; border-radius: 12px 12px 0 0; text-align: center;">
                    <h1 style="color: white; margin: 0; font-size: 20px;">SGRA - UTEQ</h1>
                    <p style="color: rgba(255,255,255,0.85); margin: 4px 0 0; font-size: 13px;">Sistema de Gestión de Refuerzos Académicos</p>
                  </div>
                  <div style="background: #ffffff; padding: 28px 24px; border: 1px solid #e0e0e0; border-top: none;">
                    <p style="color: #333; font-size: 15px; margin: 0 0 16px;">Hola <strong>%s</strong>,</p>
                    <p style="color: #555; font-size: 14px; line-height: 1.6; margin: 0 0 20px;">
                      Recibimos una solicitud para restablecer tu contraseña. Usa el siguiente código de verificación:
                    </p>
                    <div style="background: #f5f5f5; border: 2px dashed #2e7d32; border-radius: 10px; padding: 20px; text-align: center; margin: 0 0 20px;">
                      <span style="font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #2e7d32;">%s</span>
                    </div>
                    <p style="color: #777; font-size: 13px; line-height: 1.5; margin: 0 0 8px;">
                      ⏱ Este código es válido por <strong>%d minutos</strong>.
                    </p>
                    <p style="color: #777; font-size: 13px; line-height: 1.5; margin: 0;">
                      Si no solicitaste este cambio, ignora este correo. Tu contraseña permanecerá sin cambios.
                    </p>
                  </div>
                  <div style="background: #f9f9f9; padding: 16px 24px; border-radius: 0 0 12px 12px; border: 1px solid #e0e0e0; border-top: none; text-align: center;">
                    <p style="color: #999; font-size: 11px; margin: 0;">© 2026 UTEQ - Todos los derechos reservados</p>
                  </div>
                </div>
                """.formatted(firstName, code, CODE_EXPIRATION_MINUTES);
    }
}

