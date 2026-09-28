package com.uade.grandprixtracker.notification.strategy;

/**
 * Interfaz base del patrón Strategy para la construcción de notificaciones a partir
 * de eventos de dominio específicos.
 *
 * @param <T> Tipo de evento soportado
 */
public interface NotificationContentStrategy<T> {

    /**
     * Determina si esta estrategia soporta el evento recibido.
     *
     * @param event Evento a evaluar
     * @return true si la estrategia puede procesarlo
     */
    boolean supports(Object event);

    /**
     * Transforma el evento de dominio en un NotificationPayload estructurado.
     *
     * @param event Evento concreto
     * @return Payload listo para emitir
     */
    NotificationPayload createPayload(T event);
}
