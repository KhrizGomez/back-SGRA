package com.CLMTZ.Backend.service.admin.impl;

import com.CLMTZ.Backend.config.UserContextHolder;
import com.CLMTZ.Backend.dto.admin.BackupHistoryItemDTO;
import com.CLMTZ.Backend.dto.admin.BackupResultDTO;
import com.CLMTZ.Backend.dto.security.session.UserContext;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BackupServiceImpl implements IBackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupServiceImpl.class);

    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
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

    private final BlobServiceClient blobServiceClient;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${backup.container-name:backups-sgra}")
    private String backupContainerName;

    @Value("${backup.pgdump-path:}")
    private String configuredPgDumpPath;

    // -------------------------------------------------------------------------

    @Override
    public BackupResultDTO triggerManualBackup() {
        UserContext ctx = UserContextHolder.getContext();
        if (ctx == null || ctx.getDbUser() == null) {
            return fail("No hay sesión activa. Inicia sesión nuevamente.", null);
        }

        String pgDump = findPgDump();
        if (pgDump == null) {
            return fail("pg_dump no encontrado en el servidor. Verifique la instalación de PostgreSQL.", ctx.getUsername());
        }

        DbInfo db = parseUrl(datasourceUrl);
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FMT);
        String fileName  = "sgra_" + timestamp + ".backup";

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("sgra_bk_", ".backup");

            // --- Ejecutar pg_dump ---
            ProcessBuilder pb = new ProcessBuilder(
                    pgDump,
                    "-h", db.host(),
                    "-p", String.valueOf(db.port()),
                    "-U", ctx.getDbUser(),
                    "-d", db.name(),
                    "-F", "c",          // custom format (comprimido)
                    "-f", tempFile.toAbsolutePath().toString()
            );
            pb.environment().put("PGPASSWORD", ctx.getDbPassword());
            pb.environment().put("PGSSLMODE",  "require");
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String output   = new String(process.getInputStream().readAllBytes());
            boolean finished = process.waitFor(10, TimeUnit.MINUTES);

            if (!finished) {
                process.destroyForcibly();
                return fail("pg_dump superó el tiempo límite (10 min).", ctx.getUsername());
            }
            if (process.exitValue() != 0) {
                log.error("pg_dump falló [{}]: {}", process.exitValue(), output);
                return fail("pg_dump falló (código " + process.exitValue() + "): " + output, ctx.getUsername());
            }

            long fileSize = Files.size(tempFile);

            // --- Subir a Azure Blob Storage ---
            BlobContainerClient container = blobServiceClient.getBlobContainerClient(backupContainerName);
            if (!container.exists()) {
                container.create();
            }
            BlobClient blob = container.getBlobClient(BLOB_PREFIX + fileName);
            blob.uploadFromFile(tempFile.toAbsolutePath().toString(), true);

            String blobUrl = blob.getBlobUrl();
            log.info("Backup completado por '{}': {} ({} bytes)", ctx.getUsername(), fileName, fileSize);

            return new BackupResultDTO(
                    true,
                    "Respaldo completado exitosamente",
                    fileName,
                    blobUrl,
                    fileSize,
                    ctx.getUsername(),
                    LocalDateTime.now().format(DISPLAY_FMT)
            );

        } catch (Exception e) {
            log.error("Error durante backup: {}", e.getMessage(), e);
            return fail("Error inesperado: " + e.getMessage(), ctx.getUsername());
        } finally {
            if (tempFile != null) {
                try { Files.deleteIfExists(tempFile); } catch (Exception ignored) { }
            }
        }
    }

    // -------------------------------------------------------------------------

    @Override
    public List<BackupHistoryItemDTO> listBackups() {
        try {
            BlobContainerClient container = blobServiceClient.getBlobContainerClient(backupContainerName);
            if (!container.exists()) return List.of();

            return container
                    .listBlobs(new ListBlobsOptions().setPrefix(BLOB_PREFIX), null)
                    .stream()
                    .filter(item -> !item.getName().equals(BLOB_PREFIX))
                    .sorted(Comparator.comparing(
                            item -> item.getProperties().getCreationTime(),
                            Comparator.nullsLast(Comparator.reverseOrder())
                    ))
                    .map(item -> {
                        String name     = item.getName().replace(BLOB_PREFIX, "");
                        String url      = container.getBlobClient(item.getName()).getBlobUrl();
                        Long   size     = item.getProperties().getContentLength();
                        String created  = item.getProperties().getCreationTime() != null
                                ? item.getProperties().getCreationTime()
                                       .toLocalDateTime().format(DISPLAY_FMT)
                                : "—";
                        return new BackupHistoryItemDTO(name, url, size, created);
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error listando backups: {}", e.getMessage());
            return List.of();
        }
    }

    // -------------------------------------------------------------------------

    @Override
    public String validatePgDump() {
        String path = findPgDump();
        if (path == null) return null;
        try {
            Process p = new ProcessBuilder(path, "--version").start();
            String version = new String(p.getInputStream().readAllBytes()).trim();
            p.waitFor(5, TimeUnit.SECONDS);
            return version;
        } catch (Exception e) {
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

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
        } catch (Exception e) {
            return false;
        }
    }

    /** Parsea jdbc:postgresql://host:port/dbname?params */
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

    // -------------------------------------------------------------------------

    @Override
    public String getDownloadUrl(String fileName) {
        BlobContainerClient container = blobServiceClient.getBlobContainerClient(backupContainerName);
        BlobClient blob = container.getBlobClient(BLOB_PREFIX + fileName);
        if (!blob.exists()) {
            throw new RuntimeException("Archivo de respaldo no encontrado: " + fileName);
        }
        BlobSasPermission permission = new BlobSasPermission().setReadPermission(true);
        BlobServiceSasSignatureValues sasValues = new BlobServiceSasSignatureValues(
                OffsetDateTime.now().plusHours(2), permission);
        String sasToken = blob.generateSas(sasValues);
        return blob.getBlobUrl() + "?" + sasToken;
    }
}
