package com.CLMTZ.Backend.exception;

/**
 * Excepción lanzada cuando un recurso no está disponible debido a conflicto de negocio.
 * Ejemplo: franja horaria no disponible para un docente.
 */
public class TimeSlotNotAvailableException extends RuntimeException {

    public TimeSlotNotAvailableException(String message) {
        super(message);
    }

    public TimeSlotNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

