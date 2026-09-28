package com.uade.grandprixtracker.notification.channel;

import com.uade.grandprixtracker.notification.strategy.NotificationPayload;

/**
 * Interfaz base (Patrón Strategy de canal de entrega) para despachar notificaciones
 * a distintos destinos (In-App DB, Email, Push, etc.).
 */
public interface NotificationChannel {

    /**
     * Nombre identificador del canal.
     */
    String getChannelName();

    /**
     * Envía la notificación a través de este canal.
     *
     * @param payload Datos de la notificación a enviar
     */
    void send(NotificationPayload payload);
}
