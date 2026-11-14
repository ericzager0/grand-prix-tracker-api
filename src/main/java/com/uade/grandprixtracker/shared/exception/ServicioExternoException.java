package com.uade.grandprixtracker.shared.exception;

/**
 * Falla de un sistema externo (microservicio de ticketing, etc.): no responde, da timeout o devuelve un 5xx.
 * El mensaje es para los logs y puede tener detalle técnico; al cliente se le responde 503 con un texto genérico.
 */
public class ServicioExternoException extends RuntimeException {

    public ServicioExternoException(String message) {
        super(message);
    }

    public ServicioExternoException(String message, Throwable cause) {
        super(message, cause);
    }
}
