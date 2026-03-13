package com.CLMTZ.Backend.config;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.logging.Logger;

import javax.sql.DataSource;

import com.CLMTZ.Backend.dto.security.session.UserContext;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AuditAwareDataSource implements DataSource{
    
    private final DataSource delegate;

    @Override
    public Connection getConnection() throws SQLException {
        Connection conn = delegate.getConnection();
        applyAuditContext(conn);
        return conn;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection conn = delegate.getConnection(username, password);
        applyAuditContext(conn);
        return conn;
    }

    private void applyAuditContext(Connection conn) throws SQLException {
        UserContext ctx = UserContextHolder.getContext();
        if (ctx != null && ctx.getIdAuditoriaAcceso() != null) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SELECT set_config('mi_app.idauditacceso', '"
                        + ctx.getIdAuditoriaAcceso() + "', false)");
            }
        }
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        delegate.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        delegate.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegate.getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return delegate.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return delegate.isWrapperFor(iface);
    }

}
