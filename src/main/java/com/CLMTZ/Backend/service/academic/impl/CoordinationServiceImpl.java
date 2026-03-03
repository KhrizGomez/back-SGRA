package com.CLMTZ.Backend.service.academic.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.CLMTZ.Backend.dto.academic.CoordinationDTO;
import com.CLMTZ.Backend.dto.academic.StudentLoadDTO;
import com.CLMTZ.Backend.dto.academic.TeachingDTO;
import com.CLMTZ.Backend.dto.security.Response.EmailSettingsResponseDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;
import com.CLMTZ.Backend.model.academic.Coordination;
import com.CLMTZ.Backend.model.general.User;
import com.CLMTZ.Backend.model.security.EmailSettings;
import com.CLMTZ.Backend.repository.academic.ICareerRepository;
import com.CLMTZ.Backend.repository.academic.ICoordinationRepository;
import com.CLMTZ.Backend.repository.general.IUserRepository;
import com.CLMTZ.Backend.repository.security.custom.ICredentialRepository;
import com.CLMTZ.Backend.repository.security.jpa.IEmailSettingsRepository;
import com.CLMTZ.Backend.service.academic.ICoordinationService;
import com.CLMTZ.Backend.service.external.IEmailService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CoordinationServiceImpl implements ICoordinationService {

    private static final Logger log = LoggerFactory.getLogger(CoordinationServiceImpl.class);

    private final ICoordinationRepository repository;
    private final IUserRepository userRepository;
    private final ICareerRepository careerRepository;
    private final ICredentialRepository credentialRepository;
    private final IEmailSettingsRepository emailSettingsRepository;
    private final IEmailService emailService;

    @PersistenceContext
    private EntityManager entityManager;

    // --- MÉTODOS CRUD ---

    @Override
    public List<CoordinationDTO> findAll() {
        return repository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public CoordinationDTO findById(Integer id) {
        return repository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Coordination not found with id: " + id));
    }

    @Override
    public CoordinationDTO save(CoordinationDTO dto) {
        return toDTO(repository.save(toEntity(dto)));
    }

    @Override
    public CoordinationDTO update(Integer id, CoordinationDTO dto) {
        Coordination entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Coordination not found with id: " + id));
        if (dto.getUserId() != null)
            entity.setUserId(userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found")));
        if (dto.getCareerId() != null)
            entity.setCareerId(careerRepository.findById(dto.getCareerId())
                    .orElseThrow(() -> new RuntimeException("Career not found")));
        return toDTO(repository.save(entity));
    }

    @Override
    public void deleteById(Integer id) {
        repository.deleteById(id);
    }

    // --- CARGA MASIVA ---

    @Override
    public List<String> uploadStudents(List<StudentLoadDTO> dtos) {
        List<String> resultados = new ArrayList<>();

        if (dtos == null || dtos.isEmpty()) {
            resultados.add("ADVERTENCIA: No se encontraron registros válidos en el archivo. Verifique que el Excel tenga el formato correcto (datos a partir de la fila 4).");
            return resultados;
        }

        for (StudentLoadDTO fila : dtos) {
            try {
                System.out.println("Procesando Identificación: " + fila.getIdentificacion());

                String resultadoSP = ejecutarCargaEstudianteSP(
                        fila.getIdentificacion(), fila.getNombres(), fila.getApellidos(),
                        fila.getCorreo(), fila.getTelefono());

                String nombreCompleto = (fila.getNombres() + " " + fila.getApellidos()).trim();
                resultados.add("Estudiante '" + nombreCompleto + "': " + resultadoSP);

                // Si la carga fue exitosa, crear credenciales de acceso APP
                if (resultadoSP.startsWith("OK:")) {
                    try {
                        Optional<User> userOpt = userRepository.findByIdentification(fila.getIdentificacion());
                        if (userOpt.isPresent()) {
                            // idrol = 4 → Estudiante
                            SpResponseDTO credResult = credentialRepository.createNewUserCredentials(
                                    userOpt.get().getUserId(), 4);
                            if (Boolean.TRUE.equals(credResult.getSuccess())) {
                                log.info("Credenciales creadas para estudiante '{}': {}",
                                        nombreCompleto, credResult.getMessage());
                                resultados.add("  → Credenciales: " + credResult.getMessage());

                                // Enviar email con credenciales temporales
                                enviarEmailCredenciales(
                                        fila.getCorreo(),
                                        nombreCompleto,
                                        credResult.getMessage(),
                                        fila.getIdentificacion(),
                                        resultados
                                );
                            } else {
                                log.warn("Error al crear credenciales para estudiante '{}': {}",
                                        nombreCompleto, credResult.getMessage());
                                resultados.add("  → Aviso credenciales: " + credResult.getMessage());
                            }
                        }
                    } catch (Exception ce) {
                        log.error("Error al crear credenciales para estudiante '{}': {}",
                                nombreCompleto, ce.getMessage());
                        resultados.add("  → Error credenciales: " + ce.getMessage());
                    }
                }

            } catch (Exception e) {
                resultados.add("ID " + fila.getIdentificacion() + ": ERROR (" + e.getMessage() + ")");
                e.printStackTrace();
            }
        }

        long exitosos = resultados.stream().filter(r -> r.contains(": OK:")).count();
        long errores = resultados.stream().filter(r -> r.contains(": ERROR") || r.contains("FALLÓ SP:")).count();
        resultados.add(0, "RESUMEN: " + dtos.size() + " registros procesados → " + exitosos + " exitosos, " + errores + " con errores.");

        return resultados;
    }

    @Override
    public List<String> uploadTeachers(List<TeachingDTO> dtos) {
        List<String> resultados = new ArrayList<>();

        if (dtos == null || dtos.isEmpty()) {
            resultados.add("ADVERTENCIA: No se encontraron registros válidos en el archivo. Verifique que el Excel tenga el formato correcto (datos a partir de la fila 3).");
            return resultados;
        }

        for (TeachingDTO fila : dtos) {
            String nombreRef = fila.getNombreCompleto() != null ? fila.getNombreCompleto() : fila.getApellidos();
            try {
                // El SP gestiona internamente: búsqueda/creación de usuario, docente,
                // asignatura, paralelo, periodo activo y asignación de clase.
                String resultadoSP = ejecutarCargaDocenteSP(
                        fila.getNombres(), fila.getApellidos(),
                        fila.getAsignaturaTexto(), fila.getParaleloTexto());

                resultados.add("Docente '" + nombreRef + "': " + resultadoSP);

                // Si la carga fue exitosa, crear credenciales de acceso APP
                if (resultadoSP.startsWith("OK:")) {
                    try {
                        Optional<User> userOpt = userRepository.findByFirstNameAndLastName(
                                fila.getNombres(), fila.getApellidos());
                        if (userOpt.isPresent()) {
                            User docente = userOpt.get();
                            // idrol = 3 → Docente
                            SpResponseDTO credResult = credentialRepository.createNewUserCredentials(
                                    docente.getUserId(), 3);
                            if (Boolean.TRUE.equals(credResult.getSuccess())) {
                                log.info("Credenciales creadas para docente '{}': {}",
                                        nombreRef, credResult.getMessage());
                                resultados.add("  → Credenciales: " + credResult.getMessage());

                                // Enviar email con credenciales temporales
                                enviarEmailCredenciales(
                                        docente.getEmail(),
                                        nombreRef,
                                        credResult.getMessage(),
                                        docente.getIdentification(),
                                        resultados
                                );
                            } else {
                                log.warn("Error al crear credenciales para docente '{}': {}",
                                        nombreRef, credResult.getMessage());
                                resultados.add("  → Aviso credenciales: " + credResult.getMessage());
                            }
                        }
                    } catch (Exception ce) {
                        log.error("Error al crear credenciales para docente '{}': {}",
                                nombreRef, ce.getMessage());
                        resultados.add("  → Error credenciales: " + ce.getMessage());
                    }
                }

            } catch (Exception e) {
                resultados.add("Docente '" + nombreRef + "': ERROR INTERNO (" + e.getMessage() + ")");
                e.printStackTrace();
            }
        }

        long exitosos = resultados.stream().filter(r -> r.contains(": OK:")).count();
        long errores = resultados.stream().filter(r -> r.contains(": ERROR")).count();
        resultados.add(0, "RESUMEN: " + dtos.size() + " registros procesados → " + exitosos + " exitosos, " + errores + " con errores.");

        return resultados;
    }

    // --- STORED PROCEDURES ---

    private String ejecutarCargaEstudianteSP(String identificador, String nombres, String apellidos,
            String correo, String telefono) {

        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("academico.sp_in_carga_estudiante");
        query.registerStoredProcedureParameter("p_identificador", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_nombres", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_apellidos", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_correo", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_telefono", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_mensaje", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("p_exito", Boolean.class, ParameterMode.OUT);

        query.setParameter("p_identificador", identificador);
        query.setParameter("p_nombres", nombres);
        query.setParameter("p_apellidos", apellidos);
        query.setParameter("p_correo", correo);
        query.setParameter("p_telefono", telefono);
        query.execute();

        String mensaje = (String) query.getOutputParameterValue("p_mensaje");
        Boolean exito = (Boolean) query.getOutputParameterValue("p_exito");
        return Boolean.TRUE.equals(exito) ? "OK: " + mensaje : "FALLÓ SP: " + mensaje;
    }

    private String ejecutarCargaDocenteSP(String nombres, String apellidos,
            String asignatura, String paralelo) {

        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("academico.sp_in_carga_docente");
        query.registerStoredProcedureParameter("p_nombres", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_apellidos", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_asignatura", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_paralelo", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_mensaje", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("p_exito", Boolean.class, ParameterMode.OUT);

        query.setParameter("p_nombres", nombres);
        query.setParameter("p_apellidos", apellidos);
        query.setParameter("p_asignatura", asignatura);
        query.setParameter("p_paralelo", paralelo);
        query.execute();

        String mensaje = (String) query.getOutputParameterValue("p_mensaje");
        Boolean exito = (Boolean) query.getOutputParameterValue("p_exito");
        return Boolean.TRUE.equals(exito) ? "OK: " + mensaje : "FALLÓ SP: " + mensaje;
    }

    // --- EMAIL DE CREDENCIALES ---

    /**
     * Obtiene la configuración de correo activa y la convierte a EmailSettingsResponseDTO.
     */
    private Optional<EmailSettingsResponseDTO> getActiveEmailConfig() {
        log.info(">>> Buscando configuración de correo activa en BD...");
        Optional<EmailSettings> esOpt = emailSettingsRepository.findFirstByStateTrue();

        if (esOpt.isEmpty()) {
            log.warn(">>> No se encontró ninguna configuración de correo con estado=true");
            return Optional.empty();
        }

        EmailSettings es = esOpt.get();
        log.info(">>> Configuración de correo encontrada (id={}):", es.getEmailSettingId());
        log.info(">>>   SMTP Server: {}", es.getSmtpServer());
        log.info(">>>   SMTP Puerto: {}", es.getSmtpPort());
        log.info(">>>   SSL: {}", es.getSsl());
        log.info(">>>   Correo emisor: {}", es.getEmailSender());
        log.info(">>>   ⚠ [TEMPORAL] Contraseña app COMPLETA: '{}'", es.getApplicationPassword());
        log.info(">>>   Nombre remitente: {}", es.getSenderName());
        log.info(">>>   Estado: {}", es.getState());

        return Optional.of(new EmailSettingsResponseDTO(
                es.getSmtpServer(),
                es.getSmtpPort(),
                es.getSsl(),
                es.getEmailSender(),
                es.getApplicationPassword(),
                es.getSenderName()
        ));
    }

    /**
     * Envía un email con las credenciales temporales al usuario nuevo.
     * El mensaje del SP contiene el username generado.
     * La contraseña temporal es la cédula/identificador del usuario.
     */
    private void enviarEmailCredenciales(String correoDestino, String nombreCompleto,
                                          String mensajeSP, String identificador,
                                          List<String> resultados) {
        log.info("====== INICIO: Enviar email de credenciales ======");
        log.info("  Correo destino: {}", correoDestino);
        log.info("  Nombre completo: {}", nombreCompleto);
        log.info("  Mensaje SP (raw): {}", mensajeSP);
        log.info("  Identificador (cédula): {}", identificador);

        try {
            if (correoDestino == null || correoDestino.isBlank()) {
                log.warn("  ⚠ Correo destino vacío o null. No se envía email.");
                resultados.add("  → Email: No se envió (correo destino vacío)");
                return;
            }

            log.info("  Paso 1: Obteniendo configuración de correo activa...");
            Optional<EmailSettingsResponseDTO> configOpt = getActiveEmailConfig();
            if (configOpt.isEmpty()) {
                log.warn("  ⚠ No hay configuración de correo activa. No se envió email a {}", correoDestino);
                resultados.add("  → Email: No se envió (sin configuración de correo activa)");
                return;
            }
            log.info("  Paso 1: ✅ Config de correo obtenida");

            // Extraer username del mensaje del SP
            log.info("  Paso 2: Extrayendo username del mensaje del SP...");
            String username = extraerUsernameDelMensaje(mensajeSP);
            log.info("  Paso 2: Username extraído = '{}'", username);
            log.info("  ⚠ [TEMPORAL] Credenciales del usuario nuevo:");
            log.info("  ⚠ [TEMPORAL]   Username: {}", username);
            log.info("  ⚠ [TEMPORAL]   Contraseña temporal (cédula): {}", identificador);

            String subject = "SGRA - Credenciales de Acceso al Sistema";
            String body = construirEmailCredenciales(nombreCompleto, username, identificador);
            log.info("  Paso 3: Email HTML construido (longitud body: {} chars)", body.length());

            log.info("  Paso 4: Llamando a emailService.sendEmail()...");
            emailService.sendEmail(configOpt.get(), correoDestino, subject, body);
            log.info("  ✅ Email de credenciales enviado exitosamente a: {}", correoDestino);
            resultados.add("  → Email enviado a: " + correoDestino);
            log.info("====== FIN: Enviar email de credenciales (OK) ======");

        } catch (Exception e) {
            log.error("  ❌ Error al enviar email de credenciales a {}: {}", correoDestino, e.getMessage());
            log.error("  Causa raíz: {}", e.getCause() != null ? e.getCause().getMessage() : "N/A");
            resultados.add("  → Error email: " + e.getMessage());
            log.info("====== FIN: Enviar email de credenciales (ERROR) ======");
        }
    }

    /**
     * Extrae el username del mensaje del SP.
     * Formato esperado: "Credenciales creadas exitosamente. Usuario: cperezg | Contraseña temporal: ..."
     */
    private String extraerUsernameDelMensaje(String mensaje) {
        if (mensaje == null) return "N/A";
        // Buscar "Usuario: " y extraer hasta " |" o fin de string
        int idx = mensaje.indexOf("Usuario: ");
        if (idx >= 0) {
            String sub = mensaje.substring(idx + 9);
            int endIdx = sub.indexOf(" |");
            return endIdx >= 0 ? sub.substring(0, endIdx).trim() : sub.trim();
        }
        // Si es un mensaje de "Aviso: ... Username: xxx"
        idx = mensaje.indexOf("Username: ");
        if (idx >= 0) {
            return mensaje.substring(idx + 10).trim();
        }
        return "N/A";
    }

    /**
     * Construye el cuerpo HTML del email de credenciales.
     */
    private String construirEmailCredenciales(String nombre, String username, String contrasena) {
        return """
            <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: 0 auto; background: #f8f9fa; padding: 20px;">
                <div style="background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 2px 12px rgba(0,0,0,0.1);">
                    <div style="background: #2e7d32; padding: 24px; text-align: center;">
                        <h1 style="color: #ffffff; margin: 0; font-size: 20px;">Sistema de Gestión de Refuerzos Académicos</h1>
                        <p style="color: rgba(255,255,255,0.85); margin: 8px 0 0; font-size: 14px;">Universidad Técnica Estatal de Quevedo</p>
                    </div>
                    <div style="padding: 32px 24px;">
                        <p style="color: #333; font-size: 16px; margin: 0 0 16px;">Hola <strong>%s</strong>,</p>
                        <p style="color: #555; font-size: 14px; line-height: 1.6; margin: 0 0 24px;">
                            Se han creado tus credenciales de acceso al SGRA. A continuación encontrarás tu usuario y contraseña temporal:
                        </p>
                        <div style="background: #f0f7f0; border: 1px solid #c8e6c9; border-radius: 8px; padding: 20px; margin: 0 0 24px;">
                            <table style="width: 100%%; border-collapse: collapse;">
                                <tr>
                                    <td style="padding: 8px 0; color: #666; font-size: 13px; font-weight: 600;">Usuario:</td>
                                    <td style="padding: 8px 0; color: #2e7d32; font-size: 15px; font-weight: 700; font-family: monospace;">%s</td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0; color: #666; font-size: 13px; font-weight: 600;">Contraseña temporal:</td>
                                    <td style="padding: 8px 0; color: #2e7d32; font-size: 15px; font-weight: 700; font-family: monospace;">%s</td>
                                </tr>
                            </table>
                        </div>
                        <div style="background: #fff3e0; border: 1px solid #ffe0b2; border-radius: 8px; padding: 16px; margin: 0 0 24px;">
                            <p style="color: #e65100; font-size: 13px; margin: 0; line-height: 1.5;">
                                <strong>⚠ Importante:</strong> Al iniciar sesión por primera vez, el sistema te solicitará cambiar tu contraseña obligatoriamente.
                                Tu contraseña temporal es tu número de cédula/identificación.
                            </p>
                        </div>
                        <p style="color: #999; font-size: 12px; margin: 0; line-height: 1.5;">
                            Este es un correo automático generado por el SGRA. No responda a este mensaje.
                        </p>
                    </div>
                    <div style="background: #f5f5f5; padding: 16px; text-align: center; border-top: 1px solid #e0e0e0;">
                        <p style="color: #999; font-size: 11px; margin: 0;">© 2026 UTEQ - Todos los derechos reservados</p>
                    </div>
                </div>
            </div>
            """.formatted(nombre, username, contrasena);
    }

    // --- CONVERSORES DTO ---

    private CoordinationDTO toDTO(Coordination entity) {
        CoordinationDTO dto = new CoordinationDTO();
        dto.setCoordinationId(entity.getCoordinationId());
        dto.setUserId(entity.getUserId() != null ? entity.getUserId().getUserId() : null);
        dto.setCareerId(entity.getCareerId() != null ? entity.getCareerId().getCareerId() : null);
        return dto;
    }

    private Coordination toEntity(CoordinationDTO dto) {
        Coordination entity = new Coordination();
        if (dto.getUserId() != null)
            entity.setUserId(userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found")));
        if (dto.getCareerId() != null)
            entity.setCareerId(careerRepository.findById(dto.getCareerId())
                    .orElseThrow(() -> new RuntimeException("Career not found")));
        return entity;
    }
}
