package com.CLMTZ.Backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Configuration
@Profile("bootstrap-init")
public class BootstrapPsqlInitializerConfig {

    private static final Logger log = LoggerFactory.getLogger(BootstrapPsqlInitializerConfig.class);

    @Bean(name = "dataSourceScriptDatabaseInitializer")
    public InitializingBean dataSourceScriptDatabaseInitializer(
            DataSource dataSource,
            @Value("classpath:schema.sql") Resource schemaResource,
            @Value("${spring.datasource.url}") String jdbcUrl,
            @Value("${spring.sql.init.username}") String dbUser,
            @Value("${spring.sql.init.password}") String dbPassword,
            @Value("${bootstrap.psql.command:psql}") String psqlCommand,
            @Value("${bootstrap.psql.marker-function:reforzamiento.fn_get_ids_sesiones_proximas()}") String markerFunction,
            @Value("${bootstrap.psql.marker-view:seguridad.vw_gusuariosgroles}") String markerView,
            @Value("${bootstrap.psql.marker-schema:seguridad}") String markerSchema,
            @Value("${bootstrap.psql.marker-table:tbroles}") String markerTable
    ) {
        return () -> {
            boolean functionReady = functionExists(dataSource, markerFunction);
            boolean viewReady = viewExists(dataSource, markerView);

            if (functionReady && viewReady) {
                log.info("Bootstrap SQL omitido: ya existen funcion '{}' y vista '{}' de verificacion.", markerFunction, markerView);
                return;
            }

            if (tableExists(dataSource, markerSchema, markerTable)) {
                log.warn("Existe {}.{}, pero no existe {}. Se ejecutara bootstrap SQL para completar objetos faltantes.",
                        markerSchema, markerTable, markerFunction);
            } else {
                log.info("No existe tabla marcador {}.{}, se ejecutara bootstrap SQL.", markerSchema, markerTable);
            }

            JdbcTarget target = parseJdbcUrl(jdbcUrl);
            Path tempScript = copySchemaToTempFile(schemaResource);
            try {
                runPsqlScript(tempScript, target, dbUser, dbPassword, psqlCommand);
                boolean functionCreated = functionExists(dataSource, markerFunction);
                boolean viewCreated = viewExists(dataSource, markerView);
                if (!functionCreated || !viewCreated) {
                    throw new IllegalStateException(
                            "Bootstrap finalizo incompleto. Funcion OK=" + functionCreated +
                                    ", vista OK=" + viewCreated +
                                    " [funcion=" + markerFunction + ", vista=" + markerView + "]"
                    );
                }
                log.info("Bootstrap SQL ejecutado correctamente sobre base '{}'", target.database());
            } finally {
                try {
                    Files.deleteIfExists(tempScript);
                } catch (IOException ignored) {
                    // Si no puede borrar temporal no afecta el arranque.
                }
            }
        };
    }

    private boolean functionExists(DataSource dataSource, String functionSignature) throws SQLException {
        if (functionSignature == null || functionSignature.isBlank()) {
            return false;
        }

        String sql = "SELECT to_regprocedure(?) IS NOT NULL";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, functionSignature.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        }
    }

    private boolean viewExists(DataSource dataSource, String viewName) throws SQLException {
        if (viewName == null || viewName.isBlank()) {
            return false;
        }

        String sql = "SELECT to_regclass(?) IS NOT NULL";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, viewName.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        }
    }

    private boolean tableExists(DataSource dataSource, String schema, String table) throws SQLException {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.tables
                    WHERE table_schema = ? AND table_name = ?
                )
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, schema);
            stmt.setString(2, table);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        }
    }

    private Path copySchemaToTempFile(Resource schemaResource) throws IOException {
        if (!schemaResource.exists()) {
            throw new IllegalStateException("No se encontro classpath:schema.sql");
        }
        Path tempFile = Files.createTempFile("sgra-bootstrap-", ".sql");
        try (InputStream in = schemaResource.getInputStream()) {
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }
        return tempFile;
    }

    private void runPsqlScript(
            Path scriptPath,
            JdbcTarget target,
            String dbUser,
            String dbPassword,
            String psqlCommand
    ) throws IOException, InterruptedException {
        String resolvedPsql = resolvePsqlCommand(psqlCommand);

        List<String> command = new ArrayList<>();
        command.add(resolvedPsql);
        command.add("-h");
        command.add(target.host());
        command.add("-p");
        command.add(String.valueOf(target.port()));
        command.add("-U");
        command.add(dbUser);
        command.add("-d");
        command.add(target.database());
        command.add("-v");
        command.add("ON_ERROR_STOP=1");
        command.add("-f");
        command.add(scriptPath.toAbsolutePath().toString());

        ProcessBuilder pb = new ProcessBuilder(command);
        Map<String, String> env = pb.environment();
        env.put("PGPASSWORD", dbPassword);
        if (target.sslMode() != null && !target.sslMode().isBlank()) {
            env.put("PGSSLMODE", target.sslMode());
        }
        pb.redirectErrorStream(true);

        Process process = pb.start();
        String output;
        try (InputStream in = process.getInputStream()) {
            output = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        int exit = process.waitFor();
        if (exit != 0) {
            throw new IllegalStateException("Fallo bootstrap SQL por psql (exit=" + exit + "):\n" + output);
        }
        if (!output.isBlank()) {
            log.info("Salida psql bootstrap:\n{}", output);
        }
    }

    private String resolvePsqlCommand(String configuredCommand) {
        List<String> candidates = new ArrayList<>();
        if (configuredCommand != null && !configuredCommand.isBlank()) {
            candidates.add(configuredCommand.trim());
        }

        candidates.add("psql");
        candidates.add("C:/Program Files/PostgreSQL/18/bin/psql.exe");
        candidates.add("C:/Program Files/PostgreSQL/17/bin/psql.exe");
        candidates.add("C:/Program Files/PostgreSQL/16/bin/psql.exe");
        candidates.add("C:/Program Files/PostgreSQL/19/bin/psql.exe");
        candidates.add("/usr/bin/psql");
        candidates.add("/usr/local/bin/psql");

        for (String candidate : candidates) {
            try {
                Process process = new ProcessBuilder(candidate, "--version")
                        .redirectErrorStream(true)
                        .start();
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    return candidate;
                }
            } catch (Exception ignored) {
                // Continuar con el siguiente candidato.
            }
        }

        throw new IllegalStateException(
                "No se encontro el ejecutable 'psql'. Configura bootstrap.psql.command con ruta completa, " +
                        "por ejemplo: C:/Program Files/PostgreSQL/17/bin/psql.exe"
        );
    }

    private JdbcTarget parseJdbcUrl(String jdbcUrl) {
        String prefix = "jdbc:postgresql://";
        if (!jdbcUrl.startsWith(prefix)) {
            throw new IllegalArgumentException("URL JDBC PostgreSQL no soportada: " + jdbcUrl);
        }

        String withoutPrefix = jdbcUrl.substring(prefix.length());
        String[] parts = withoutPrefix.split("\\?", 2);
        String hostAndDb = parts[0];
        Map<String, String> params = parseParams(parts.length > 1 ? parts[1] : "");

        int slash = hostAndDb.indexOf('/');
        if (slash < 0 || slash == hostAndDb.length() - 1) {
            throw new IllegalArgumentException("No se pudo obtener base de datos desde URL JDBC: " + jdbcUrl);
        }

        String hostPort = hostAndDb.substring(0, slash);
        String database = hostAndDb.substring(slash + 1);

        String host;
        int port;
        int colon = hostPort.lastIndexOf(':');
        if (colon > 0) {
            host = hostPort.substring(0, colon);
            port = Integer.parseInt(hostPort.substring(colon + 1));
        } else {
            host = hostPort;
            port = 5432;
        }

        return new JdbcTarget(host, port, database, params.get("sslmode"));
    }

    private Map<String, String> parseParams(String rawParams) {
        Map<String, String> result = new HashMap<>();
        if (rawParams == null || rawParams.isBlank()) {
            return result;
        }

        String[] pairs = rawParams.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                result.put(kv[0].toLowerCase(Locale.ROOT), kv[1]);
            }
        }
        return result;
    }

    private record JdbcTarget(String host, int port, String database, String sslMode) {
    }
}

