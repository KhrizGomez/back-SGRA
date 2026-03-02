package com.CLMTZ.Backend.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Pool de conexiones dinámico por usuario.
 * Mantiene un HikariDataSource por cada dbUser con cache y TTL.
 */
@Component
public class UserConnectionPool {

    private static final Logger log = LoggerFactory.getLogger(UserConnectionPool.class);

    private final Map<String, PoolEntry> userPools = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Value("${sgra.pool.max-size-per-user:3}")
    private int maxPoolSizePerUser;

    @Value("${sgra.pool.idle-timeout-minutes:10}")
    private int idleTimeoutMinutes;

    @Value("${sgra.pool.max-lifetime-minutes:30}")
    private int maxLifetimeMinutes;

    @Value("${sgra.pool.cleanup-interval-minutes:5}")
    private int cleanupIntervalMinutes;

    public UserConnectionPool() {
        // Programar limpieza periódica de pools inactivos
        cleanupExecutor.scheduleAtFixedRate(
                this::cleanupInactivePools,
                5, 5, TimeUnit.MINUTES
        );
    }

    /**
     * Obtiene o crea un DataSource para el usuario especificado.
     */
    public DataSource getDataSource(String dbUser, String dbPassword) {
        if (dbUser == null || dbPassword == null) {
            throw new IllegalArgumentException("dbUser y dbPassword no pueden ser null");
        }

        return userPools.compute(dbUser, (key, existing) -> {
            if (existing != null && !existing.dataSource.isClosed()) {
                existing.lastAccess = System.currentTimeMillis();
                return existing;
            }
            // Crear nuevo pool
            log.info("Creando nuevo pool de conexiones para usuario: {}", dbUser);
            HikariDataSource ds = createDataSource(dbUser, dbPassword);
            return new PoolEntry(ds);
        }).dataSource;
    }

    /**
     * Elimina el pool de un usuario específico (ej: en logout).
     */
    public void evict(String dbUser) {
        if (dbUser == null) return;

        PoolEntry entry = userPools.remove(dbUser);
        if (entry != null) {
            log.info("Eliminando pool de conexiones para usuario: {}", dbUser);
            closeQuietly(entry.dataSource);
        }
    }

    /**
     * Obtiene estadísticas de los pools activos.
     */
    public Map<String, PoolStats> getPoolStats() {
        Map<String, PoolStats> stats = new ConcurrentHashMap<>();
        userPools.forEach((user, entry) -> {
            HikariDataSource ds = entry.dataSource;
            if (!ds.isClosed()) {
                stats.put(user, new PoolStats(
                        ds.getHikariPoolMXBean().getActiveConnections(),
                        ds.getHikariPoolMXBean().getIdleConnections(),
                        ds.getHikariPoolMXBean().getTotalConnections(),
                        entry.lastAccess
                ));
            }
        });
        return stats;
    }

    private HikariDataSource createDataSource(String dbUser, String dbPassword) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.setDriverClassName(driverClassName);

        // Configuración del pool
        config.setMaximumPoolSize(maxPoolSizePerUser);
        config.setMinimumIdle(1);
        config.setIdleTimeout(TimeUnit.MINUTES.toMillis(idleTimeoutMinutes));
        config.setMaxLifetime(TimeUnit.MINUTES.toMillis(maxLifetimeMinutes));
        config.setConnectionTimeout(30000); // 30 segundos

        // Nombre del pool para identificación
        config.setPoolName("UserPool-" + dbUser);

        // Configuración adicional para PostgreSQL
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        return new HikariDataSource(config);
    }

    private void cleanupInactivePools() {
        long now = System.currentTimeMillis();
        long maxIdleTime = TimeUnit.MINUTES.toMillis(maxLifetimeMinutes);

        userPools.entrySet().removeIf(entry -> {
            PoolEntry poolEntry = entry.getValue();
            boolean shouldRemove = (now - poolEntry.lastAccess) > maxIdleTime
                    || poolEntry.dataSource.isClosed();

            if (shouldRemove) {
                log.info("Limpiando pool inactivo para usuario: {}", entry.getKey());
                closeQuietly(poolEntry.dataSource);
            }
            return shouldRemove;
        });
    }

    private void closeQuietly(HikariDataSource ds) {
        try {
            if (ds != null && !ds.isClosed()) {
                ds.close();
            }
        } catch (Exception e) {
            log.warn("Error al cerrar DataSource: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Cerrando todos los pools de conexión de usuarios...");
        cleanupExecutor.shutdown();
        userPools.values().forEach(entry -> closeQuietly(entry.dataSource));
        userPools.clear();
    }

    /**
     * Entrada de cache con timestamp de último acceso.
     */
    private static class PoolEntry {
        final HikariDataSource dataSource;
        volatile long lastAccess;

        PoolEntry(HikariDataSource dataSource) {
            this.dataSource = dataSource;
            this.lastAccess = System.currentTimeMillis();
        }
    }

    /**
     * DTO para estadísticas de pool.
     */
    public record PoolStats(int activeConnections, int idleConnections, int totalConnections, long lastAccess) {}
}

