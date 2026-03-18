package com.CLMTZ.Backend.service.admin.impl;

import com.CLMTZ.Backend.config.EmergencyDbConfig;
import com.CLMTZ.Backend.config.UserContextHolder;
import com.CLMTZ.Backend.dto.admin.BackupBrowseDTO;
import com.CLMTZ.Backend.dto.admin.BackupHistoryItemDTO;
import com.CLMTZ.Backend.dto.admin.BackupLocalConfigDTO;
import com.CLMTZ.Backend.dto.admin.BackupResultDTO;
import com.CLMTZ.Backend.dto.admin.BackupScheduleEntryDTO;
import com.CLMTZ.Backend.dto.security.session.UserContext;
import com.CLMTZ.Backend.model.admin.BackupLocalConfig;
import com.CLMTZ.Backend.model.admin.BackupScheduleEntry;
import com.CLMTZ.Backend.repository.admin.IBackupLocalConfigRepository;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class BackupServiceImpl implements IBackupService, ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(BackupServiceImpl.class);

    private final EmergencyDbConfig emergencyDbConfig;

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
    private final IBackupLocalConfigRepository localConfigRepository;
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
    public BackupResultDTO restoreBackup(String fileName, Boolean cero) {
        UserContext ctx = UserContextHolder.getContext();
        String executedBy = ctx != null ? ctx.getUsername() : "Sistema";

        String pgRestore = findPgRestore();
        if (pgRestore == null) return fail("pg_restore no encontrado en el servidor.", executedBy);

        BlobContainerClient container = blobServiceClient.getBlobContainerClient(backupContainerName);
        BlobClient blob = container.getBlobClient(BLOB_PREFIX + fileName);
        if (!blob.exists()) return fail("Archivo no encontrado: " + fileName, executedBy);

        Path tempZip = null;
        Path tempBackup = null;
        boolean schemasRenombrados = false;
        try {
            tempZip = Files.createTempFile("sgra_restore_zip_", ".zip");
            tempBackup = Files.createTempFile("sgra_restore_bk_", ".backup");

            final Path finalTempFile = tempZip;
            CompletableFuture<Void> descarga = CompletableFuture.runAsync(() -> {
                try (java.io.InputStream blobStream = blob.openInputStream()) {
                    Files.copy(blobStream, finalTempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {
                    throw new RuntimeException("Error descargando backup desde Azure: " + e.getMessage(), e);
                }
            });

            // Renombra schemas a _bak mientras descarga (permite rollback si falla el restore)
            if (!cero) {
                renombrarSchemas();
                schemasRenombrados = true;
            }
            

            // Espera que termine la descarga antes de lanzar pg_restore
            descarga.join();

            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(tempZip.toFile()))) {
                ZipEntry zipEntry = zis.getNextEntry();
                if (zipEntry != null) {
                    try (FileOutputStream fos = new FileOutputStream(tempBackup.toFile())) {
                        zis.transferTo(fos);
                    }
                } else {
                    rollbackSchemas();
                    return fail("El archivo ZIP está vacío o es inválido.", executedBy);
                }
            }

            DbInfo db = parseUrl(datasourceUrl);
            int jobs = Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors()));
            log.info("Iniciando pg_restore con {} workers paralelos.", jobs);

            ProcessBuilder pb = new ProcessBuilder(
                    pgRestore,
                    "-h", db.host(), "-p", String.valueOf(db.port()),
                    "-U", backupDbUser, "-d", db.name(),
                    "--no-owner", "--disable-triggers",
                    "-j", String.valueOf(jobs),
                    "-F", "c",
                    tempBackup.toAbsolutePath().toString()
            );
            pb.environment().put("PGPASSWORD", backupDbPassword);
            pb.environment().put("PGSSLMODE",  "require");
            pb.redirectErrorStream(true);

            Process process  = pb.start();
            String  output   = new String(process.getInputStream().readAllBytes());
            boolean finished = process.waitFor(30, TimeUnit.MINUTES);

            if (!finished) {
                process.destroyForcibly();
                rollbackSchemas();
                return fail("pg_restore superó el tiempo límite.", executedBy);
            }
            if (process.exitValue() != 0) {
                log.error("pg_restore falló [{}]: {}", process.exitValue(), output);
                rollbackSchemas();
                return fail("pg_restore falló: " + output, executedBy);
            }

            // Restore exitoso → elimina los schemas _bak que ya no se necesitan
            if(!cero) eliminarSchemasBak();
            log.info("Restauración '{}' completada por '{}'.", fileName, executedBy);

            entityManagerFactory.getCache().evictAll();
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
            if (schemasRenombrados) {
                try { rollbackSchemas(); } catch (Exception re) {
                    log.error("Error en rollback de schemas: {}", re.getMessage());
                }
            }
            return fail("Error inesperado durante la restauración: " + e.getMessage(), executedBy);
        } finally {
            if (tempZip != null) try { Files.deleteIfExists(tempZip); } catch (Exception ignored) { }
            if (tempBackup != null) try { Files.deleteIfExists(tempBackup); } catch (Exception ignored) { }
        }
    }

    // ─── Restauración a nueva base de datos ────────────────────────────────────

    @Override
    public BackupResultDTO restoreBackupToNewDatabase(String fileName, Boolean connect) {
        UserContext ctx = UserContextHolder.getContext();
        String executedBy = ctx != null ? ctx.getUsername() : "Sistema";

        String pgRestore = findPgRestore();
        if (pgRestore == null) return fail("pg_restore no encontrado en el servidor.", executedBy);

        String psql = findPsql();
        if (psql == null) return fail("psql no encontrado en el servidor.", executedBy);

        BlobContainerClient container = blobServiceClient.getBlobContainerClient(backupContainerName);
        BlobClient blob = container.getBlobClient(BLOB_PREFIX + fileName);
        if (!blob.exists()) return fail("Archivo no encontrado: " + fileName, executedBy);

        String newDbName = "";
        if(!connect) {
            newDbName = fileName.replace(".zip", "").replace(".backup", "");
            newDbName = newDbName.replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase();
        }
        else {
            DbInfo db = parseUrl(datasourceUrl);
            newDbName = db.name();
        }

        Path tempZip = null;
        Path tempBackup = null;
        try {
            tempZip = Files.createTempFile("sgra_restore_newdb_zip_", ".zip");
            tempBackup = Files.createTempFile("sgra_restore_newdb_bk_", ".backup");

            log.info("Descargando backup '{}' desde Azure para crear nueva BD '{}'...", fileName, newDbName);
            try (java.io.InputStream blobStream = blob.openInputStream()) {
                Files.copy(blobStream, tempZip, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(tempZip.toFile()))) {
                ZipEntry zipEntry = zis.getNextEntry();
                if (zipEntry != null) {
                    try (FileOutputStream fos = new FileOutputStream(tempBackup.toFile())) {
                        zis.transferTo(fos);
                    }
                } else {
                    return fail("El archivo ZIP está vacío o es inválido.", executedBy);
                }
            }

            DbInfo db = parseUrl(datasourceUrl);

            if (databaseExists(db, newDbName)) {
                return fail("Ya existe una base de datos con el nombre: " + newDbName, executedBy);
            }

            log.info("Creando nueva base de datos '{}'...", newDbName);
            ProcessBuilder pbCreate = new ProcessBuilder(
                    psql,
                    "-h", db.host(), "-p", String.valueOf(db.port()),
                    "-U", backupDbUser, "-d", "postgres",
                    "-c", "CREATE DATABASE \\\"" + newDbName + "\\\" WITH OWNER = \\\"" + backupDbUser + "\\\" ENCODING = 'UTF8';"
            );
            pbCreate.environment().put("PGPASSWORD", backupDbPassword);
            pbCreate.environment().put("PGSSLMODE", "require");
            pbCreate.redirectErrorStream(true);

            Process createProcess = pbCreate.start();
            String createOutput = new String(createProcess.getInputStream().readAllBytes());
            boolean createFinished = createProcess.waitFor(2, TimeUnit.MINUTES);

            if (!createFinished) {
                createProcess.destroyForcibly();
                return fail("Creación de base de datos superó el tiempo límite.", executedBy);
            }
            if (createProcess.exitValue() != 0) {
                log.error("Error creando BD [{}]: {}", createProcess.exitValue(), createOutput);
                return fail("Error creando la base de datos: " + createOutput, executedBy);
            }

            log.info("Base de datos '{}' creada exitosamente.", newDbName);

            int jobs = Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors()));
            log.info("Iniciando pg_restore con {} workers paralelos en BD '{}'.", jobs, newDbName);

            ProcessBuilder pbRestore = new ProcessBuilder(
                    pgRestore,
                    "-h", db.host(), "-p", String.valueOf(db.port()),
                    "-U", backupDbUser, "-d", newDbName,
                    "--no-owner", "--disable-triggers",
                    "-j", String.valueOf(jobs),
                    "-F", "c",
                    tempBackup.toAbsolutePath().toString()
            );
            pbRestore.environment().put("PGPASSWORD", backupDbPassword);
            pbRestore.environment().put("PGSSLMODE", "require");
            pbRestore.redirectErrorStream(true);

            Process restoreProcess = pbRestore.start();
            String restoreOutput = new String(restoreProcess.getInputStream().readAllBytes());
            boolean restoreFinished = restoreProcess.waitFor(30, TimeUnit.MINUTES);

            if (!restoreFinished) {
                restoreProcess.destroyForcibly();
                dropDatabase(db, newDbName);
                return fail("pg_restore superó el tiempo límite.", executedBy);
            }
            if (restoreProcess.exitValue() != 0) {
                log.error("pg_restore falló [{}]: {}", restoreProcess.exitValue(), restoreOutput);
                dropDatabase(db, newDbName);
                return fail("pg_restore falló: " + restoreOutput, executedBy);
            }

            log.info("Restauración en nueva BD '{}' completada por '{}'.", newDbName, executedBy);

            return new BackupResultDTO(true,
                    "Base de datos '" + newDbName + "' creada y restaurada exitosamente desde " + fileName,
                    fileName, null, null, executedBy,
                    LocalDateTime.now().format(DISPLAY_FMT));

        } catch (Exception e) {
            log.error("Error durante restauración a nueva BD: {}", e.getMessage(), e);
            return fail("Error inesperado durante la restauración: " + e.getMessage(), executedBy);
        } finally {
            if (tempZip != null) try { Files.deleteIfExists(tempZip); } catch (Exception ignored) { }
            if (tempBackup != null) try { Files.deleteIfExists(tempBackup); } catch (Exception ignored) { }
        }
    }

    private boolean databaseExists(DbInfo db, String dbName) {
        try {
            String psql = findPsql();
            if (psql == null) return false;

            ProcessBuilder pb = new ProcessBuilder(
                    psql,
                    "-h", db.host(), "-p", String.valueOf(db.port()),
                    "-U", backupDbUser, "-d", "postgres",
                    "-tAc", "SELECT 1 FROM pg_database WHERE datname = '" + dbName + "';"
            );
            pb.environment().put("PGPASSWORD", backupDbPassword);
            pb.environment().put("PGSSLMODE", "require");
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes()).trim();
            process.waitFor(30, TimeUnit.SECONDS);

            return "1".equals(output);
        } catch (Exception e) {
            log.error("Error verificando existencia de BD '{}': {}", dbName, e.getMessage());
            return false;
        }
    }

    /** Elimina una base de datos (para limpieza si el restore falla). */
    private void dropDatabase(DbInfo db, String dbName) {
        try {
            String psql = findPsql();
            if (psql == null) return;

            ProcessBuilder pb = new ProcessBuilder(
                    psql,
                    "-h", db.host(), "-p", String.valueOf(db.port()),
                    "-U", backupDbUser, "-d", "postgres",
                    "-c", "DROP DATABASE IF EXISTS \"" + dbName + "\";"
            );
            pb.environment().put("PGPASSWORD", backupDbPassword);
            pb.environment().put("PGSSLMODE", "require");
            pb.redirectErrorStream(true);

            Process process = pb.start();
            process.waitFor(1, TimeUnit.MINUTES);
            log.info("Base de datos '{}' eliminada (limpieza post-error).", dbName);
        } catch (Exception e) {
            log.error("Error eliminando BD '{}': {}", dbName, e.getMessage());
        }
    }

    /** Busca el ejecutable psql (mismo directorio que pg_dump). */
    private String findPsql() {
        String pgDump = findPgDump();
        if (pgDump == null) return null;
        String psql = pgDump.replace("pg_dump", "psql");
        try {
            Process p = new ProcessBuilder(psql, "--version").start();
            p.waitFor(5, TimeUnit.SECONDS);
            return p.exitValue() == 0 ? psql : null;
        } catch (Exception e) { return null; }
    }

    // ─── Backup automático ─────────────────────────────────────────────────────

    private void runScheduledBackup(Integer scheduleId) {
        log.info("Ejecutando backup automático para schedule #{}...", scheduleId);
        scheduleRepository.findById(scheduleId).ifPresent(entry -> {
            BackupResultDTO result = executeBackup(backupDbUser, backupDbPassword,
                    "Sistema (automático — schedule #" + scheduleId + ")");
            entry.setFechaUltimaEjecucion(LocalDateTime.now());
            entry.setResultadoUltimaEjecucion(
                    result.isSuccess() ? "OK: " + result.getFileName()
                                       : "ERROR: " + result.getMessage()
            );
            scheduleRepository.save(entry);
            // Copia local (regla 3-2-1): guarda en ruta configurada si existe
            if (result.isSuccess() && result.getFileName() != null) {
                saveToLocalPath(result.getFileName());
            }
        });
    }

    /** Guarda el archivo de backup en la ruta local configurada (solo para respaldos automáticos). */
    private void saveToLocalPath(String fileName) {
        localConfigRepository.findById(1).ifPresentOrElse(config -> {
            try {
                java.nio.file.Path dir  = java.nio.file.Path.of(config.getRuta());
                java.nio.file.Files.createDirectories(dir);
                java.nio.file.Path dest = dir.resolve(fileName);
                BlobContainerClient container = blobServiceClient.getBlobContainerClient(backupContainerName);
                BlobClient blob = container.getBlobClient(BLOB_PREFIX + fileName);
                try (java.io.InputStream blobStream = blob.openInputStream()) {
                    java.nio.file.Files.copy(blobStream, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                log.info("Backup '{}' guardado localmente en '{}'.", fileName, config.getRuta());
            } catch (Exception e) {
                log.error("No se pudo guardar backup local en '{}': {}", config.getRuta(), e.getMessage(), e);
            }
        }, () -> log.warn("saveToLocalPath: ruta local no configurada — copia local omitida."));
    }

    // ─── Ejecución compartida ──────────────────────────────────────────────────

    private BackupResultDTO executeBackup(String dbUser, String dbPassword, String executedBy) {
        String pgDump = findPgDump();
        if (pgDump == null) return fail("pg_dump no encontrado en el servidor.", executedBy);

        DbInfo db = parseUrl(datasourceUrl);
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FMT);
        String fileNameBack  = "sgra_" + timestamp + ".backup";
        String zipFileName    = "sgra_" + timestamp + ".zip";

        Path tempFileBackup = null;
        Path tempZip = null;
        try {
            tempFileBackup = Files.createTempFile("sgra_bk_", ".backup");
            tempZip = Files.createTempFile("sgra_zip_", ".zip");

            ProcessBuilder pb = new ProcessBuilder(
                    pgDump,
                    "-h", db.host(), "-p", String.valueOf(db.port()),
                    "-U", dbUser, "-d", db.name(),
                    "-F", "c", "-f", tempFileBackup.toAbsolutePath().toString()
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

            try(ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempZip.toFile())); FileInputStream inputFile = new FileInputStream(tempFileBackup.toFile())){
                ZipEntry zipEntry = new ZipEntry(fileNameBack);
                zos.putNextEntry(zipEntry);
                inputFile.transferTo(zos);
                zos.closeEntry();
            }

            long fileSize = Files.size(tempZip);
            BlobContainerClient container = blobServiceClient.getBlobContainerClient(backupContainerName);
            if (!container.exists()) container.create();

            BlobClient blob = container.getBlobClient(BLOB_PREFIX + zipFileName);
            blob.uploadFromFile(tempZip.toAbsolutePath().toString(), true);
            log.info("Backup '{}' comprimido y completado por '{}' ({} bytes).", zipFileName, executedBy, fileSize);

            return new BackupResultDTO(true, "Respaldo completado exitosamente",
                    zipFileName, blob.getBlobUrl(), fileSize, executedBy,
                    LocalDateTime.now().format(DISPLAY_FMT));

        } catch (Exception e) {
            log.error("Error durante backup: {}", e.getMessage(), e);
            return fail("Error inesperado: " + e.getMessage(), executedBy);
        } finally {
            if (tempFileBackup != null) try { Files.deleteIfExists(tempFileBackup); } catch (Exception ignored) { }
            if (tempZip != null) try { Files.deleteIfExists(tempZip); } catch (Exception ignored) { }
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
    public void streamBackup(String fileName, java.io.OutputStream out) throws Exception {
        BlobContainerClient container = blobServiceClient.getBlobContainerClient(backupContainerName);
        BlobClient blob = container.getBlobClient(BLOB_PREFIX + fileName);
        if (!blob.exists()) throw new RuntimeException("Archivo no encontrado: " + fileName);
        try (java.io.InputStream blobStream = blob.openInputStream()) {
            blobStream.transferTo(out);
        }
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
        entry.setMeses(dto.getMeses() != null ? dto.getMeses() : "*");
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

    // ─── Configuración ruta local ──────────────────────────────────────────────

    @Override
    public BackupLocalConfigDTO getLocalConfig() {
        return localConfigRepository.findById(1)
                .map(c -> new BackupLocalConfigDTO(
                        c.getRuta(),
                        c.getUsuario() != null ? c.getUsuario().getUserId() : null,
                        c.getFechaConfiguracion() != null ? c.getFechaConfiguracion().format(DISPLAY_FMT) : null
                ))
                .orElse(null);
    }

    @Override
    public BackupLocalConfigDTO saveLocalConfig(String ruta) {
        BackupLocalConfig config = localConfigRepository.findById(1).orElse(new BackupLocalConfig());
        config.setId(1);
        config.setRuta(ruta);
        config.setFechaConfiguracion(LocalDateTime.now());
        UserContext ctx = UserContextHolder.getContext();
        if (ctx != null && ctx.getUserId() != null) {
            userRepository.findById(ctx.getUserId()).ifPresent(config::setUsuario);
        }
        BackupLocalConfig saved = localConfigRepository.save(config);
        return new BackupLocalConfigDTO(
                saved.getRuta(),
                saved.getUsuario() != null ? saved.getUsuario().getUserId() : null,
                saved.getFechaConfiguracion() != null ? saved.getFechaConfiguracion().format(DISPLAY_FMT) : null
        );
    }

    @Override
    public BackupBrowseDTO browseDirectory(String path) {
        // Sin path → devuelve las raíces del sistema de archivos (C:\, D:\, / etc.)
        if (path == null || path.isBlank()) {
            List<String> roots = Arrays.stream(File.listRoots())
                    .map(File::getAbsolutePath)
                    .sorted()
                    .collect(Collectors.toList());
            return new BackupBrowseDTO("", null, roots);
        }

        Path dir = Path.of(path);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            throw new RuntimeException("Ruta no válida: " + path);
        }

        List<String> subdirs;
        try (var stream = Files.list(dir)) {
            subdirs = stream
                    .filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .filter(name -> !name.startsWith("."))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            subdirs = List.of();
        }

        String parentPath = dir.getParent() != null ? dir.getParent().toString() : null;
        return new BackupBrowseDTO(dir.toAbsolutePath().toString(), parentPath, subdirs);
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    /** Genera la expresión cron a partir de los campos estructurados. */
    private String toCron(BackupScheduleEntry e) {
        return switch (e.getFrecuencia()) {
            case "SEMANAL" -> String.format("0 %d %d * * %s",
                    e.getMinuto(), e.getHora(),
                    (e.getDiaSemana() != null && !e.getDiaSemana().isBlank()) ? e.getDiaSemana() : "MON");
            case "MENSUAL" -> {
                String dias  = (e.getDiaMes() != null && !e.getDiaMes().isBlank()) ? e.getDiaMes() : "1";
                String meses = (e.getMeses()  != null && !e.getMeses().isBlank())  ? e.getMeses()  : "*";
                yield String.format("0 %d %d %s %s *", e.getMinuto(), e.getHora(), dias, meses);
            }
            default -> String.format("0 %d %d * * *", e.getMinuto(), e.getHora());
        };
    }

    private BackupScheduleEntryDTO toDTO(BackupScheduleEntry e) {
        return new BackupScheduleEntryDTO(
                e.getId(),
                e.getUsuario() != null ? e.getUsuario().getUserId() : null,
                e.isHabilitado(), e.getFrecuencia(),
                e.getDiaSemana(), e.getDiaMes(), e.getMeses(),
                e.getHora(), e.getMinuto(),
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
        e.setMeses(dto.getMeses() != null ? dto.getMeses() : "*");
        e.setHora(dto.getHora());
        e.setMinuto(dto.getMinuto());
        return e;
    }

    /**
     * Renombra todos los schemas de usuario a {nombre}_bak antes del restore.
     * Permite rollback: si pg_restore falla, se pueden restaurar los schemas originales.
     */
    private void renombrarSchemas() throws Exception {
        try (java.sql.Connection        conn = dataSource.getConnection();
             java.sql.CallableStatement cs   = conn.prepareCall("CALL general.sp_bk_renombrar_schemas()")) {
            cs.execute();
            log.info("Schemas renombrados a _bak — pg_restore los recreará desde el backup.");
        }
    }

    /**
     * Rollback: elimina los schemas parciales del restore fallido
     * y renombra los _bak de vuelta a sus nombres originales.
     */
    private void rollbackSchemas() throws Exception {
        try (java.sql.Connection        conn = dataSource.getConnection();
             java.sql.CallableStatement cs   = conn.prepareCall("CALL general.sp_bk_rollback_schemas()")) {
            cs.execute();
            log.info("Rollback de schemas completado — BD restaurada al estado anterior.");
        }
    }

    /**
     * Tras restore exitoso: elimina los schemas _bak que ya no se necesitan.
     */
    private void eliminarSchemasBak() throws Exception {
        try (java.sql.Connection        conn = dataSource.getConnection();
             java.sql.CallableStatement cs   = conn.prepareCall("CALL general.sp_bk_limpiar_schemas_bak()")) {
            cs.execute();
            log.info("Schemas _bak eliminados tras restauración exitosa.");
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

    @Override
    public Boolean restoreDropBd(String fileName){
        HikariDataSource tempDs = null;
        try {
            DbInfo db = parseUrl(datasourceUrl);
            if (databaseExists(db,db.name())) return false;
            JdbcTemplate emergency = emergencyDbConfig.emergencyDataSource();
            tempDs = (HikariDataSource) emergency.getDataSource();
            String dbName = "\"" + db.name() + "\"";
            emergency.execute("CREATE DATABASE " + dbName);
            BackupResultDTO response = restoreBackup(fileName, true);
            return response.isSuccess();
        } catch (Exception e) {
            throw new RuntimeException("Erro al volver a crear la bd: "+e.getCause().getMessage());
        } finally {
            if( tempDs != null && !tempDs.isClosed()){
                tempDs.close();
            }
        }
    }
}