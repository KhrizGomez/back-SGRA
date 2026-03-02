package com.CLMTZ.Backend.config;

import com.CLMTZ.Backend.dto.security.session.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

/**
 * Servicio que proporciona acceso a conexiones dinámicas de BD por usuario.
 * Utiliza el UserContext del ThreadLocal para obtener las credenciales del usuario actual.
 */
@Service
public class DynamicDataSourceService {

    private static final Logger log = LoggerFactory.getLogger(DynamicDataSourceService.class);

    private final UserConnectionPool connectionPool;
    private final DataSource defaultDataSource; // Fallback para operaciones del sistema

    public DynamicDataSourceService(UserConnectionPool connectionPool, DataSource defaultDataSource) {
        this.connectionPool = connectionPool;
        this.defaultDataSource = defaultDataSource;
    }

    /**
     * Obtiene un NamedParameterJdbcTemplate con la conexión del usuario actual.
     * Si no hay contexto de usuario, usa la conexión por defecto del sistema.
     *
     * @return NamedParameterJdbcTemplate configurado con las credenciales apropiadas
     */
    public NamedParameterJdbcTemplate getJdbcTemplate() {
        UserContext ctx = UserContextHolder.getContext();

        if (ctx == null || ctx.getDbUser() == null || ctx.getDbPassword() == null) {
            log.debug("No hay contexto de usuario, usando conexión por defecto");
            return new NamedParameterJdbcTemplate(defaultDataSource);
        }

        DataSource userDataSource = connectionPool.getDataSource(ctx.getDbUser(), ctx.getDbPassword());
        return new NamedParameterJdbcTemplate(userDataSource);
    }

    /**
     * Obtiene el DataSource del usuario actual.
     *
     * @return DataSource del usuario o el default si no hay contexto
     */
    public DataSource getDataSource() {
        UserContext ctx = UserContextHolder.getContext();

        if (ctx == null || ctx.getDbUser() == null || ctx.getDbPassword() == null) {
            log.debug("No hay contexto de usuario, usando DataSource por defecto");
            return defaultDataSource;
        }

        return connectionPool.getDataSource(ctx.getDbUser(), ctx.getDbPassword());
    }

    /**
     * Obtiene el DataSource por defecto del sistema (sgra_app).
     * Usar solo para operaciones que no requieren contexto de usuario.
     */
    public DataSource getDefaultDataSource() {
        return defaultDataSource;
    }

    /**
     * Obtiene un JdbcTemplate con la conexión por defecto del sistema.
     * Usar solo para operaciones que no requieren contexto de usuario (login, health checks).
     */
    public NamedParameterJdbcTemplate getDefaultJdbcTemplate() {
        return new NamedParameterJdbcTemplate(defaultDataSource);
    }

    /**
     * Verifica si hay un contexto de usuario activo con credenciales de BD.
     */
    public boolean hasUserContext() {
        UserContext ctx = UserContextHolder.getContext();
        return ctx != null && ctx.getDbUser() != null && ctx.getDbPassword() != null;
    }

    /**
     * Obtiene el nombre del usuario de BD actual (para logging/auditoría).
     */
    public String getCurrentDbUser() {
        UserContext ctx = UserContextHolder.getContext();
        return ctx != null ? ctx.getDbUser() : "sgra_app";
    }
}

