package com.CLMTZ.Backend.service.academic.impl;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.CLMTZ.Backend.config.DynamicDataSourceService;

import com.CLMTZ.Backend.dto.academic.CoordinationDTO;
import com.CLMTZ.Backend.dto.academic.StudentLoadDTO;
import com.CLMTZ.Backend.dto.academic.TeachingDTO;
import com.CLMTZ.Backend.dto.security.Response.SpResponseDTO;
import com.CLMTZ.Backend.model.academic.Class;
import com.CLMTZ.Backend.model.academic.Coordination;
import com.CLMTZ.Backend.model.academic.Teaching;
import com.CLMTZ.Backend.model.general.User;
import com.CLMTZ.Backend.repository.academic.ICareerRepository;
import com.CLMTZ.Backend.repository.academic.IClassRepository;
import com.CLMTZ.Backend.repository.academic.ICoordinationRepository;
import com.CLMTZ.Backend.repository.academic.ITeachingRepository;
import com.CLMTZ.Backend.repository.general.IUserRepository;
import com.CLMTZ.Backend.repository.security.custom.ICredentialRepository;
import com.CLMTZ.Backend.repository.security.jpa.IAccessRepository;
import com.CLMTZ.Backend.service.academic.ICoordinationService;
import com.CLMTZ.Backend.service.external.IEmailService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CoordinationServiceImpl implements ICoordinationService {

    private static final Logger log = LoggerFactory.getLogger(CoordinationServiceImpl.class);

    private final ICoordinationRepository repository;
    private final IUserRepository userRepository;
    private final ICareerRepository careerRepository;
    private final ICredentialRepository credentialRepository;
    private final IAccessRepository accessRepository;
    private final IEmailService emailService;
    private final DynamicDataSourceService dynamicDataSourceService;
    private final IClassRepository classRepository;
    private final ITeachingRepository teachingRepository;

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
            resultados.add("ADVERTENCIA: No se encontraron registros válidos para procesar.");
            return resultados;
        }

        try {
            // Convertir la lista de DTOs a JSON para el SP
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String jsonData = mapper.writeValueAsString(dtos);

            System.out.println("[UPLOAD-STUDENTS] Enviando " + dtos.size() + " registros al SP como JSON");
            String resultadoSP = ejecutarCargaEstudianteSP(jsonData);
            resultados.add(resultadoSP);

            // Si el SP procesó correctamente, crear credenciales y enviar emails
            if (resultadoSP.startsWith("OK:")) {
                for (StudentLoadDTO fila : dtos) {
                    String nombreCompleto = (fila.getNombres() + " " + fila.getApellidos()).trim();
                    try {
                        Optional<User> userOpt = userRepository.findByIdentification(fila.getIdentificacion());
                        if (userOpt.isPresent()) {
                            if (accessRepository.existsByUser_UserId(userOpt.get().getUserId())) {
                                log.info("Credenciales ya existen para estudiante '{}'. Se omite creación y email.", nombreCompleto);
                                resultados.add("  → Aviso credenciales: ya existen (no se envió email)");
                                continue;
                            }
                            // rol → Estudiante (por nombre, no por ID)
                            SpResponseDTO credResult = credentialRepository.createNewUserCredentials(
                                    userOpt.get().getUserId(), "Estudiante");
                            if (Boolean.TRUE.equals(credResult.getSuccess())) {
                                log.info("Credenciales creadas para estudiante '{}': {}",
                                        nombreCompleto, credResult.getMessage());
                                resultados.add("  → Credenciales [" + nombreCompleto + "]: " + credResult.getMessage());

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
                                resultados.add("  → Aviso credenciales [" + nombreCompleto + "]: " + credResult.getMessage());
                            }
                        }
                    } catch (Exception ce) {
                        log.error("Error al crear credenciales para estudiante '{}': {}",
                                nombreCompleto, ce.getMessage());
                        resultados.add("  → Error credenciales [" + nombreCompleto + "]: " + ce.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            resultados.add("ERROR GENERAL: " + e.getMessage());
            e.printStackTrace();
        }
        // Línea de resumen comentada por solicitud del usuario
        // resultados.add(0, "RESUMEN: " + dtos.size() + " registros procesados → " + exitosos + " exitosos, " + errores + " con errores.");

        return resultados;
    }

    @Override
    public List<String> uploadTeachers(List<TeachingDTO> dtos) {
        List<String> resultados = new ArrayList<>();

        if (dtos == null || dtos.isEmpty()) {
            resultados.add("ADVERTENCIA: No se encontraron registros válidos en el archivo. Verifique que el Excel tenga el formato correcto (datos a partir de la fila 3).");
            return resultados;
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String jsonData = mapper.writeValueAsString(dtos);

            System.out.println("[UPLOAD-TEACHERS] Enviando " + dtos.size() + " registros al SP como JSON");
            String resultadoSP = ejecutarCargaDocenteSP(jsonData);
            resultados.add(resultadoSP);

            // Si el SP procesó correctamente, crear credenciales, enviar emails Y actualizar docente en clases
            if (resultadoSP.startsWith("OK:")) {
                for (TeachingDTO fila : dtos) {
                    String nombreRef = fila.getNombreCompleto() != null ? fila.getNombreCompleto() : fila.getApellidos();
                    try {
                        Optional<User> userOpt = userRepository.findByFirstNameAndLastName(
                                fila.getNombres(), fila.getApellidos());
                        if (userOpt.isPresent()) {
                            User docente = userOpt.get();
                            if (accessRepository.existsByUser_UserId(docente.getUserId())) {
                                log.info("Credenciales ya existen para docente '{}'. Se omite creación y email.", nombreRef);
                                resultados.add("  → Aviso credenciales: ya existen (no se envió email)");
                            } else {
                                // rol → Docente (por nombre, no por ID)
                                SpResponseDTO credResult = credentialRepository.createNewUserCredentials(
                                        docente.getUserId(), "Docente");
                                if (Boolean.TRUE.equals(credResult.getSuccess())) {
                                    log.info("Credenciales creadas para docente '{}': {}",
                                            nombreRef, credResult.getMessage());
                                    resultados.add("  → Credenciales [" + nombreRef + "]: " + credResult.getMessage());

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
                                    resultados.add("  → Aviso credenciales [" + nombreRef + "]: " + credResult.getMessage());
                                }
                            }

                            // Actualizar docente en la clase si la materia+paralelo+periodo activo ya existía con otro docente
                            actualizarDocenteEnClase(fila, docente, resultados, nombreRef);
                        }
                    } catch (Exception ce) {
                        log.error("Error al procesar docente '{}': {}",
                                nombreRef, ce.getMessage());
                        resultados.add("  → Error procesando [" + nombreRef + "]: " + ce.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            resultados.add("ERROR GENERAL: " + e.getMessage());
            e.printStackTrace();
        }
        // Línea de resumen comentada por solicitud del usuario
        // resultados.add(0, "RESUMEN: " + dtos.size() + " registros procesados → " + exitosos + " exitosos, " + errores + " con errores.");

        return resultados;
    }

    // --- STORED PROCEDURES ---

    /**
     * Si la clase (materia+paralelo+periodo activo) ya existe con un docente diferente,
     * actualiza el docente al nuevo. Esto cubre el caso de cambio de docente en carga Excel.
     */
    private void actualizarDocenteEnClase(TeachingDTO fila, User docente,
                                           List<String> resultados, String nombreRef) {
        try {
            String asignatura = fila.getAsignaturaTexto();
            String paralelo   = fila.getParaleloTexto();
            if (asignatura == null || asignatura.isBlank() || paralelo == null || paralelo.isBlank()) return;

            Optional<Class> claseOpt = classRepository
                    .findBySubjectId_SubjectIgnoreCaseAndParallelId_SectionIgnoreCaseAndPeriodId_StateTrue(
                            asignatura.trim(), paralelo.trim());

            if (claseOpt.isEmpty()) return; // clase no existe aún, el SP se encargó de crearla

            Class clase = claseOpt.get();
            Optional<Teaching> teachingOpt = teachingRepository.findByUserId_UserId(docente.getUserId());
            if (teachingOpt.isEmpty()) return; // el docente aún no tiene registro teaching

            Teaching nuevoDocente = teachingOpt.get();
            Teaching docenteActual = clase.getTeacherId();

            if (docenteActual != null && docenteActual.getTeachingId().equals(nuevoDocente.getTeachingId())) {
                return; // ya está asignado el mismo docente, nada que actualizar
            }

            // El docente cambió: actualizar la clase
            String anteriorNombre = docenteActual != null && docenteActual.getUserId() != null
                    ? docenteActual.getUserId().getFirstName() + " " + docenteActual.getUserId().getLastName()
                    : "sin docente";
            clase.setTeacherId(nuevoDocente);
            classRepository.save(clase);
            log.info("Clase '{}' paralelo '{}': docente actualizado de '{}' a '{}'",
                    asignatura, paralelo, anteriorNombre, nombreRef);
            resultados.add("  → Docente actualizado en clase [" + asignatura + " / " + paralelo + "]: "
                    + anteriorNombre + " → " + nombreRef);

        } catch (Exception e) {
            log.warn("No se pudo verificar/actualizar docente en clase para '{}': {}", nombreRef, e.getMessage());
        }
    }

    private String ejecutarCargaEstudianteSP(String jsonData) {
        try {
            String sql = "CALL academico.sp_in_carga_estudiante(?, ?, ?)";
            JdbcTemplate jdbcTemplate = dynamicDataSourceService.getJdbcTemplate().getJdbcTemplate();

            return jdbcTemplate.execute(
                (Connection con) -> {
                    CallableStatement cs = con.prepareCall(sql);
                    cs.setObject(1, jsonData, Types.OTHER);
                    cs.registerOutParameter(2, Types.VARCHAR);
                    cs.registerOutParameter(3, Types.BOOLEAN);
                    return cs;
                },
                (CallableStatement cs) -> {
                    cs.execute();
                    String mensaje = cs.getString(2);
                    Boolean exito = cs.getBoolean(3);
                    log.info("sp_in_carga_estudiante → exito={}, mensaje={}", exito, mensaje);
                    return Boolean.TRUE.equals(exito) ? "OK: " + mensaje : "FALLÓ SP: " + mensaje;
                }
            );

        } catch (Exception e) {
            String causaMsg = e.getCause() != null && e.getCause().getMessage() != null
                    ? e.getCause().getMessage() : e.getMessage();
            log.error("Error al ejecutar SP carga estudiante: {}", causaMsg, e);
            return "FALLÓ SP: Error interno: " + causaMsg;
        }
    }

    private String ejecutarCargaDocenteSP(String jsonData) {
        try {
            String sql = "CALL academico.sp_in_carga_docente(?, ?, ?)";
            JdbcTemplate jdbcTemplate = dynamicDataSourceService.getJdbcTemplate().getJdbcTemplate();

            return jdbcTemplate.execute(
                (Connection con) -> {
                    CallableStatement cs = con.prepareCall(sql);
                    cs.setObject(1, jsonData, Types.OTHER);
                    cs.registerOutParameter(2, Types.VARCHAR);
                    cs.registerOutParameter(3, Types.BOOLEAN);
                    return cs;
                },
                (CallableStatement cs) -> {
                    cs.execute();
                    String mensaje = cs.getString(2);
                    Boolean exito = cs.getBoolean(3);
                    log.info("sp_in_carga_docente → exito={}, mensaje={}", exito, mensaje);
                    return Boolean.TRUE.equals(exito) ? "OK: " + mensaje : "FALLÓ SP: " + mensaje;
                }
            );

        } catch (Exception e) {
            String causaMsg = e.getCause() != null && e.getCause().getMessage() != null
                    ? e.getCause().getMessage() : e.getMessage();
            log.error("Error al ejecutar SP carga docente: {}", causaMsg, e);
            return "FALLÓ SP: Error interno: " + causaMsg;
        }
    }

    // --- EMAIL DE CREDENCIALES ---

    /**
     * Envía un email con las credenciales temporales al usuario nuevo.
     * El mensaje del SP contiene el username generado.
     * La contraseña temporal es la cédula/identificador del usuario.
     * Usa el servicio general de correo que obtiene la config activa automáticamente.
     */
    private void enviarEmailCredenciales(String correoDestino, String nombreCompleto,
                                          String mensajeSP, String identificador,
                                          List<String> resultados) {
        try {
            if (correoDestino == null || correoDestino.isBlank()) {
                resultados.add("  → Email: No se envió (correo destino vacío)");
                return;
            }

            String username = extraerUsernameDelMensaje(mensajeSP);
            String subject = "SGRA - Credenciales de Acceso al Sistema";
            String body = construirEmailCredenciales(nombreCompleto, username, identificador);

            emailService.sendEmailAsync(correoDestino, subject, body);
            resultados.add("  → Email enviando a: " + correoDestino + " (en segundo plano)");

        } catch (Exception e) {
            log.error("Error al enviar email de credenciales a {}: {}", correoDestino, e.getMessage());
            resultados.add("  → Error email: " + e.getMessage());
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
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0; padding:0; background:#F4F7F5; font-family:'Segoe UI',Roboto,Arial,sans-serif;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background:#F4F7F5; padding:32px 16px;">
                    <tr><td align="center">
                        <table width="580" cellpadding="0" cellspacing="0" style="background:#ffffff; border-radius:16px; overflow:hidden; box-shadow:0 4px 24px rgba(0,0,0,0.08);">

                            <!-- HEADER -->
                            <tr><td style="background:linear-gradient(135deg, #0D4F32 0%%, #0F5B3B 50%%, #116A43 100%%); padding:32px 24px; text-align:center;">
                                <h1 style="color:#ffffff; margin:0 0 4px; font-size:22px; font-weight:700; letter-spacing:-0.3px;">SGRA</h1>
                                <p style="color:rgba(255,255,255,0.75); margin:0; font-size:12px; font-weight:400; letter-spacing:0.5px; text-transform:uppercase;">Sistema de Gestión de Refuerzos Académicos</p>
                                <p style="color:rgba(255,255,255,0.6); margin:6px 0 0; font-size:11px;">Universidad Técnica Estatal de Quevedo</p>
                            </td></tr>

                            <!-- BODY -->
                            <tr><td style="padding:32px 28px 24px;">
                                <p style="color:#1a1a2e; font-size:16px; margin:0 0 8px; font-weight:600;">¡Bienvenido/a, %s!</p>
                                <p style="color:#555770; font-size:14px; line-height:1.6; margin:0 0 24px;">
                                    Se ha creado tu cuenta en el SGRA. A continuación encontrarás tus credenciales de acceso:
                                </p>

                                <!-- CREDENCIALES -->
                                <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f8faf9; border:1px solid #e0e8e4; border-radius:12px; overflow:hidden; margin:0 0 24px;">
                                    <tr><td style="padding:16px 20px; border-bottom:1px solid #e0e8e4;">
                                        <table width="100%%" cellpadding="0" cellspacing="0">
                                            <tr>
                                                <td style="color:#6c757d; font-size:13px; font-weight:500; width:140px;">👤 Usuario</td>
                                                <td style="color:#0D4F32; font-size:15px; font-weight:700; font-family:'Courier New',monospace; letter-spacing:0.5px;">%s</td>
                                            </tr>
                                        </table>
                                    </td></tr>
                                    <tr><td style="padding:16px 20px;">
                                        <table width="100%%" cellpadding="0" cellspacing="0">
                                            <tr>
                                                <td style="color:#6c757d; font-size:13px; font-weight:500; width:140px;">🔑 Contraseña</td>
                                                <td style="color:#0D4F32; font-size:15px; font-weight:700; font-family:'Courier New',monospace; letter-spacing:0.5px;">%s</td>
                                            </tr>
                                        </table>
                                    </td></tr>
                                </table>

                                <!-- AVISO -->
                                <table width="100%%" cellpadding="0" cellspacing="0" style="background:#FFF8E1; border-left:4px solid #F9A825; border-radius:0 8px 8px 0; margin:0 0 24px;">
                                    <tr><td style="padding:14px 16px;">
                                        <p style="color:#7B6418; font-size:13px; margin:0; line-height:1.5;">
                                            <strong>⚠ Importante:</strong> Al iniciar sesión por primera vez, se te solicitará cambiar tu contraseña. Tu contraseña temporal es tu número de cédula.
                                        </p>
                                    </td></tr>
                                </table>

                                <p style="color:#8b8da3; font-size:12px; margin:0; line-height:1.5; text-align:center;">
                                    Este es un correo automático del SGRA. No responda a este mensaje.
                                </p>
                            </td></tr>

                            <!-- FOOTER -->
                            <tr><td style="background:#f8faf9; padding:16px 24px; text-align:center; border-top:1px solid #e8ece9;">
                                <p style="color:#8b8da3; font-size:11px; margin:0;">© 2026 UTEQ — Todos los derechos reservados</p>
                            </td></tr>

                        </table>
                    </td></tr>
                </table>
            </body>
            </html>
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
