package com.CLMTZ.Backend.config;

import com.CLMTZ.Backend.dto.security.session.UserContext;

/**
 * ThreadLocal holder para el UserContext del request actual.
 * Permite acceder al contexto del usuario autenticado desde cualquier parte del código
 * sin necesidad de pasar el HttpSession explícitamente.
 */
public class UserContextHolder {

    private static final ThreadLocal<UserContext> contextHolder = new ThreadLocal<>();

    private UserContextHolder() {
        // Utility class
    }

    /**
     * Establece el UserContext para el thread actual.
     */
    public static void setContext(UserContext context) {
        contextHolder.set(context);
    }

    /**
     * Obtiene el UserContext del thread actual.
     * @return UserContext o null si no hay sesión activa
     */
    public static UserContext getContext() {
        return contextHolder.get();
    }

    /**
     * Limpia el UserContext del thread actual.
     * IMPORTANTE: Llamar siempre al final del request para evitar memory leaks.
     */
    public static void clear() {
        contextHolder.remove();
    }

    /**
     * Verifica si hay un contexto de usuario activo.
     */
    public static boolean hasContext() {
        return contextHolder.get() != null;
    }

    /**
     * Obtiene el dbUser del contexto actual.
     * @return dbUser o null si no hay contexto
     */
    public static String getDbUser() {
        UserContext ctx = contextHolder.get();
        return ctx != null ? ctx.getDbUser() : null;
    }

    /**
     * Obtiene el dbPassword del contexto actual.
     * @return dbPassword o null si no hay contexto
     */
    public static String getDbPassword() {
        UserContext ctx = contextHolder.get();
        return ctx != null ? ctx.getDbPassword() : null;
    }
}

