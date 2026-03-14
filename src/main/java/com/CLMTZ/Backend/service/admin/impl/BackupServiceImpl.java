package com.CLMTZ.Backend.service.admin.impl;

import com.CLMTZ.Backend.config.UserContextHolder;
import com.CLMTZ.Backend.dto.admin.BackupHistoryItemDTO;
import com.CLMTZ.Backend.dto.admin.BackupResultDTO;
import com.CLMTZ.Backend.dto.admin.BackupScheduleEntryDTO;
import com.CLMTZ.Backend.dto.security.session.UserContext;
import com.CLMTZ.Backend.model.admin.BackupScheduleEntry;
import com.CLMTZ.Backend.repository.admin.IBackupScheduleEntryRepository;
import com.CLMTZ.Backend.repository.general.IUserRepository;
import com.CLMTZ.Backend.service.admin.BackupScheduler;
import com.CLMTZ.Backend.service.admin.IBackupService;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BackupServiceImpl implements IBackupService, ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(BackupServiceImpl.class);

    // Include seconds/millis to avoid overwriting backups created within the same minute.
    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    private static final DateTimeFormatter DISPLAY_FMT   = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String BLOB_PREFIX = "backups/";

    private static final List<String> PGDUMP_CANDIDATES = List.of(
            "pg_dump",
            "C:/Program Files/PostgreSQL/18/bin/pg_dump.exe",
            "C:/Program Files/PostgreSQL/17/bin/pg_dump.exe",
            "C:/Program Files/PostgreSQL/19/bin/pg_dump.exe",
            "C:/Program Files/PostgreSQL/16/bin/pg_dump.exe",
            "/usr/bin/pg_dump",
            "/usr/local/bin/pg_dump"
    );

    // Constructor injection (Lombok)
    private final BlobServiceClient blobServiceClient;
    private final IBackupScheduleEntryRepository scheduleRepository;
    private final IUserRepository userRepository;
    private final BackupScheduler backupScheduler;
    private final DataSource dataSource;
    private final EntityManagerFactory entityManagerFactory;

    // @Value fields (Spring injection after constructor)
    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${backup.db-username}")
    private String backupDbUser;

    @Value("${backup.db-password}")
    private String backupDbPassword;

    @Value("${backup.container-name:backups-sgra}")
    private String backupContainerName;

    @Value("${backup.pgdump-path:}")
    private String configuredPgDumpPath;

    // ─── Arranque ──────────────────────────────────────────────────────────────

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        scheduleRepository.findAll().forEach(entry ->
                backupScheduler.applyEntry(entry.getId(), entry.isHabilitado(),
                        toCron(entry), () -> runScheduledBackup(entry.getId()))
        );
        log.info("Backup schedules cargados: {} entradas.", scheduleRepository.count());
    }

    // ─── Backup manual ─────────────────────────────────────────────────────────

    @Override
    public BackupResultDTO triggerManualBackup() {

        UserContext ctx = UserContextHolder.getContext();
        if (ctx == null || ctx.getDbUser() == null) {
            return fail("No hay sesión activa. Inicia sesión nuevamente.", null);
        }
        return executeBackup(ctx.getDbUser(), ctx.getDbPassword(), ctx.getUsername());
    }

    // ─── Restauración ──────────────────────────────────────────────────────────

    @Override
    public BackupResultDTO restoreBackup(String fileName) {
        UserContext ctx = UserContextHolder.getContext();
        String executedBy = ctx != null ? ctx.getUsername() : "Sistema";

        String pgRestore = findPgRestore();
        if (pgRestore == null) return fail("pg_restore no encontrado en el servidor.", executedBy);

        BlobContainerClient container = blobServiceClient.getBlobContainerClient(backupContainerName);
        BlobClient blob = container.getBlobClient(BLOB_PREFIX + fileName);
        if (!blob.exists()) return fail("Archivo no encontrado: " + fileName, executedBy);

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("sgra_restore_", ".backup");
            // openInputStream() usa IO bloqueante, evita el error de ByteBuffer de Netty
            try (java.io.InputStream blobStream = blob.openInputStream()) {
                Files.copy(blobStream, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            // Limpia todos los schemas de usuario con CASCADE antes de restaurar.
            // Esto elimina todas las dependencias (FKs, índices, etc.) sin importar
            // si existen o no en el backup, evitando errores de "depende de ella".
            dropUserSchemas();

            DbInfo db = parseUrl(datasourceUrl);
            ProcessBuilder pb = new ProcessBuilder(
                    pgRestore,
                    "-h", db.host(), "-p", String.valueOf(db.port()),
                    "-U", backupDbUser, "-d", db.name(),
                    "--single-transaction", "--no-owner",
                    "-F", "c",
                    tempFile.toAbsolutePath().toString()
            );
            pb.environment().put("PGPASSWORD", backupDbPassword);
            pb.environment().put("PGSSLMODE",  "require");
            pb.redirectErrorStream(true);

            Process process  = pb.start();
            String  output   = new String(process.getInputStream().readAllBytes());
            boolean finished = process.waitFor(30, TimeUnit.MINUTES);

            if (!finished) { process.destroyForcibly(); return fail("pg_restore superó el tiempo límite.", executedBy); }
            if (process.exitValue() != 0) {
                log.error("pg_restore falló [{}]: {}", process.exitValue(), output);
                return fail("pg_restore falló: " + output, executedBy);
            }

            log.info("Restauración '{}' completada por '{}'.", fileName, executedBy);

            // Limpia el caché de entidades de Hibernate para que todo se re-lea desde la BD restaurada
            entityManagerFactory.getCache().evictAll();

            // Marca todas las conexiones del pool para reemplazo (se recrean con la BD restaurada)
            if (dataSource instanceof HikariDataSource hikari) {
                hikari.getHikariPoolMXBean().softEvictConnections();
            }

            log.info("Caché de Hibernate y pool de conexiones reseteados post-restauración.");
            return new BackupResultDTO(true,
                    "Base de datos restaurada exitosamente desde " + fileName,
                    fileName, null, null, executedBy,
                    LocalDateTime.now().format(DISPLAY_FMT));

        } catch (Exception e) {
            log.error("Error durante restauración: {}", e.getMessage(), e);
            return fail("Error inesperado durante la restauración: " + e.getMessage(), executedBy);
        } finally {
            if (tempFile != null) try { Files.deleteIfExists(tempFile); } catch (Exception ignored) { }
        }
    }

    // ─── Backup automático ─────────────────────────────────────────────────────

    private void runScheduledBackup(Integer scheduleId) {
        log.info("Ejecutando backup automático para schedule #{}...", scheduleId);
        BackupResultDTO result = executeBackup(backupDbUser, backupDbPassword,
                "Sistema (automático — schedule #" + scheduleId + ")");

        scheduleRepository.findById(scheduleId).ifPresent(entry -> {
            entry.setFechaUltimaEjecucion(LocalDateTime.now());
            entry.setResultadoUltimaEjecucion(
                    result.isSuccess() ? "OK: " + result.getFileName()
                                       : "ERROR: " + result.getMessage()
            );
            scheduleRepository.save(entry);
        });
    }

    // ─── Ejecución compartida ──────────────────────────────────────────────────

    private BackupResultDTO executeBackup(String dbUser, String dbPassword, String executedBy) {
        String pgDump = findPgDump();
        if (pgDump == null) return fail("pg_dump no encontrado en el servidor.", executedBy);

        DbInfo db = parseUrl(datasourceUrl);
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FMT);
        String fileName  = "sgra_" + timestamp + ".backup";

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("sgra_bk_", ".backup");

            ProcessBuilder pb = new ProcessBuilder(
                    pgDump,
                    "-h", db.host(), "-p", String.valueOf(db.port()),
                    "-U", dbUser, "-d", db.name(),
                    "-F", "c", "-f", tempFile.toAbsolutePath().toString()
            );
            pb.environment().put("PGPASSWORD", dbPassword);
            pb.environment().put("PGSSLMODE",  "require");
            pb.redirectErrorStream(true);

            Process process  = pb.start();
            String output    = new String(process.getInputStream().readAllBytes());
            boolean finished = process.waitFor(10, TimeUnit.MINUTES);

            if (!finished) { process.destroyForcibly(); return fail("pg_dump superó el tiempo límite.", executedBy); }
            if (process.exitValue() != 0) {
                log.error("pg_dump falló [{}]: {}", process.exitValue(), output);
                return fail("pg_dump falló: " + output, executedBy);
            }

            long fileSize = Files.size(tempFile);
            BlobContainerClient container = blobServiceClient.getBlobContainerClient(backupContainerName);
            if (!container.exists()) container.create();

            BlobClient blob = container.getBlobClient(BLOB_PREFIX + fileName);
            blob.uploadFromFile(tempFile.toAbsolutePath().toString(), true);

            log.info("Backup '{}' completado por '{}' ({} bytes).", fileName, executedBy, fileSize);
            return new BackupResultDTO(true, "Respaldo completado exitosamente",
                    fileName, blob.getBlobUrl(), fileSize, executedBy,
                    LocalDateTime.now().format(DISPLAY_FMT));

        } catch (Exception e) {
            log.error("Error durante backup: {}", e.getMessage(), e);
            return fail("Error inesperado: " + e.getMessage(), executedBy);
        } finally {
            if (tempFile != null) try { Files.deleteIfExists(tempFile); } catch (Exception ignored) { }
        }
    }

    // ─── Historial y descarga ──────────────────────────────────────────────────

    @Override
    public List<BackupHistoryItemDTO> listBackups() {
        try {
            BlobContainerClient container = blobServiceClient.getBlobContainerClient(backupContainerName);
            if (!container.exists()) return List.of();

            return container.listBlobs(new ListBlobsOptions().setPrefix(BLOB_PREFIX), null)
                    .stream()
                    .filter(item -> !item.getName().equals(BLOB_PREFIX))
                    .sorted(Comparator.comparing(
                            item -> item.getProperties().getCreationTime(),
                            Comparator.nullsLast(Comparator.reverseOrder())
                    ))
                    .map(item -> new BackupHistoryItemDTO(
                            item.getName().replace(BLOB_PREFIX, ""),
                            container.getBlobClient(item.getName()).getBlobUrl(),
                            item.getProperties().getContentLength(),
                            item.getProperties().getCreationTime() != null
                                    ? item.getProperties().getCreationTime()
                                            .atZoneSameInstant(ZoneId.systemDefault())
                                            .toLocalDateTime().format(DISPLAY_FMT)
                                    : "—"
                    ))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error listando backups: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public String validatePgDump() {
        String path = findPgDump();
        if (path == null) return null;
        try {
            Process p = new ProcessBuilder(path, "--version").start();
            String version = new String(p.getInputStream().readAllBytes()).trim();
            p.waitFor(5, TimeUnit.SECONDS);
            return version;
        } catch (Exception e) { return null; }
    }

    @Override
    public void deleteBackup(String fileName) {
        BlobContainerClient container = blobServiceClient.getBlobContainerClient(backupContainerName);
        BlobClient blob = container.getBlobClient(BLOB_PREFIX + fileName);
        if (!blob.exists()) throw new RuntimeException("Archivo no encontrado: " + fileName);
        blob.delete();
        log.info("Backup '{}' eliminado.", fileName);
    }

    @Override
    public String getDownloadUrl(String fileName) {
        BlobContainerClient container = blobServiceClient.getBlobContainerClient(backupContainerName);
        BlobClient blob = container.getBlobClient(BLOB_PREFIX + fileName);
        if (!blob.exists()) throw new RuntimeException("Archivo no encontrado: " + fileName);
        BlobSasPermission permission = new BlobSasPermission().setReadPermission(true);
        BlobServiceSasSignatureValues sasValues = new BlobServiceSasSignatureValues(
                OffsetDateTime.now().plusHours(2), permission);
        return blob.getBlobUrl() + "?" + blob.generateSas(sasValues);
    }

    // ─── CRUD de schedules ─────────────────────────────────────────────────────

    @Override
    public List<BackupScheduleEntryDTO> listSchedules() {
        return scheduleRepository.findAll().stream()
                .sorted(Comparator.comparing(BackupScheduleEntry::getId))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public BackupScheduleEntryDTO createSchedule(BackupScheduleEntryDTO dto) {
        BackupScheduleEntry draft = fromDTO(dto);
        draft.setFechaCreacion(LocalDateTime.now());
        UserContext ctx = UserContextHolder.getContext();
        if (ctx != null && ctx.getUserId() != null) {
            userRepository.findById(ctx.getUserId()).ifPresent(draft::setUsuario);
        }
        final BackupScheduleEntry saved = scheduleRepository.save(draft);
        backupScheduler.applyEntry(saved.getId(), saved.isHabilitado(),
                toCron(saved), () -> runScheduledBackup(saved.getId()));
        return toDTO(saved);
    }

    @Override
    public BackupScheduleEntryDTO updateSchedule(Integer id, BackupScheduleEntryDTO dto) {
        BackupScheduleEntry entry = scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule no encontrado: " + id));
        entry.setHabilitado(dto.isHabilitado());
        entry.setFrecuencia(dto.getFrecuencia());
        entry.setDiaSemana(dto.getDiaSemana());
        entry.setDiaMes(dto.getDiaMes());
        entry.setHora(dto.getHora());
        entry.setMinuto(dto.getMinuto());
        final BackupScheduleEntry updated = scheduleRepository.save(entry);
        backupScheduler.applyEntry(updated.getId(), updated.isHabilitado(),
                toCron(updated), () -> runScheduledBackup(updated.getId()));
        return toDTO(updated);
    }

    @Override
    public void deleteSchedule(Integer id) {
        backupScheduler.removeEntry(id);
        scheduleRepository.deleteById(id);
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    /** Genera la expresión cron a partir de los campos estructurados. */
    private String toCron(BackupScheduleEntry e) {
        return switch (e.getFrecuencia()) {
            case "SEMANAL"  -> String.format("0 %d %d * * %s", e.getMinuto(), e.getHora(),
                                e.getDiaSemana() != null ? e.getDiaSemana() : "SUN");
            case "MENSUAL"  -> String.format("0 %d %d %d * *", e.getMinuto(), e.getHora(),
                                e.getDiaMes()    != null ? e.getDiaMes()    : 1);
            default          -> String.format("0 %d %d * * *",  e.getMinuto(), e.getHora()); // DIARIO
        };
    }

    private BackupScheduleEntryDTO toDTO(BackupScheduleEntry e) {
        return new BackupScheduleEntryDTO(
                e.getId(),
                e.getUsuario() != null ? e.getUsuario().getUserId() : null,
                e.isHabilitado(), e.getFrecuencia(),
                e.getDiaSemana(), e.getDiaMes(), e.getHora(), e.getMinuto(),
                e.getFechaUltimaEjecucion() != null ? e.getFechaUltimaEjecucion().format(DISPLAY_FMT) : null,
                e.getResultadoUltimaEjecucion()
        );
    }

    private BackupScheduleEntry fromDTO(BackupScheduleEntryDTO dto) {
        BackupScheduleEntry e = new BackupScheduleEntry();
        e.setHabilitado(dto.isHabilitado());
        e.setFrecuencia(dto.getFrecuencia() != null ? dto.getFrecuencia() : "DIARIO");
        e.setDiaSemana(dto.getDiaSemana());
        e.setDiaMes(dto.getDiaMes());
        e.setHora(dto.getHora());
        e.setMinuto(dto.getMinuto());
        return e;
    }

    /**
     * Elimina todos los schemas de usuario con CASCADE (excluye pg_*, information_schema y public).
     * NO los recrea — pg_restore los crea desde el backup.
     * El DROP CASCADE elimina tablas, FKs e índices sin importar dependencias.
     */
    private void dropUserSchemas() throws Exception {
        String sql = """
                DO $$
                DECLARE r RECORD;
                BEGIN
                    FOR r IN
                        SELECT nspname FROM pg_namespace
                        WHERE nspname NOT LIKE 'pg_%'
                          AND nspname NOT IN ('information_schema', 'public')
                    LOOP
                        EXECUTE 'DROP SCHEMA IF EXISTS ' || quote_ident(r.nspname) || ' CASCADE';
                    END LOOP;
                END $$;
                """;
        try (java.sql.Connection conn = dataSource.getConnection();
             java.sql.Statement  stmt = conn.createStatement()) {
            stmt.execute(sql);
            log.info("Schemas de usuario eliminados — pg_restore los recreará desde el backup.");
        }
    }

    /** Deriva la ruta de pg_restore desde la de pg_dump (siempre están en el mismo directorio). */
    private String findPgRestore() {
        String pgDump = findPgDump();
        if (pgDump == null) return null;
        String pgRestore = pgDump.replace("pg_dump", "pg_restore");
        try {
            Process p = new ProcessBuilder(pgRestore, "--version").start();
            p.waitFor(5, TimeUnit.SECONDS);
            return p.exitValue() == 0 ? pgRestore : null;
        } catch (Exception e) { return null; }
    }

    private String findPgDump() {
        if (configuredPgDumpPath != null && !configuredPgDumpPath.isBlank()) {
            if (isExecutable(configuredPgDumpPath)) return configuredPgDumpPath;
        }
        for (String candidate : PGDUMP_CANDIDATES) {
            if (isExecutable(candidate)) return candidate;
        }
        return null;
    }

    private boolean isExecutable(String path) {
        try {
            Process p = new ProcessBuilder(path, "--version").start();
            p.waitFor(5, TimeUnit.SECONDS);
            return p.exitValue() == 0;
        } catch (Exception e) { return false; }
    }

    private DbInfo parseUrl(String url) {
        String stripped = url.replace("jdbc:postgresql://", "");
        String hostPort = stripped.split("/")[0];
        String dbPart   = stripped.split("/")[1];
        String dbName   = dbPart.contains("?") ? dbPart.split("\\?")[0] : dbPart;
        String host     = hostPort.contains(":") ? hostPort.split(":")[0] : hostPort;
        int    port     = hostPort.contains(":") ? Integer.parseInt(hostPort.split(":")[1]) : 5432;
        return new DbInfo(host, port, dbName);
    }

    private BackupResultDTO fail(String message, String user) {
        return new BackupResultDTO(false, message, null, null, null, user,
                LocalDateTime.now().format(DISPLAY_FMT));
    }

    private record DbInfo(String host, int port, String name) { }
}
