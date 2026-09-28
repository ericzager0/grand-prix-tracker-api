package com.uade.grandprixtracker.notification.channel;

import com.uade.grandprixtracker.notification.strategy.NotificationPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Canal de notificación por Email: simula el despacho de un correo electrónico
 * registrando la operación en los logs del sistema.
 */
@Component
public class EmailNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationChannel.class);

    @Override
    public String getChannelName() {
        return "EMAIL";
    }

    @Override
    public void send(NotificationPayload payload) {
        String recipient;
        String dispatchMode;
        if (payload.isPubSub()) {
            recipient = "newsletter-subscribers@grandprixtracker.com (Lista masiva - Pub/Sub)";
            dispatchMode = "PUB/SUB (Broadcast)";
        } else {
            recipient = (payload.userId() != null ? "usuario-" + payload.userId() : "usuario-anonimo") + "@grandprixtracker.com";
            dispatchMode = "PUNTO A PUNTO (Unicast)";
        }

        log.info("================== [SIMULACIÓN EMAIL - {}] ==================", dispatchMode);
        log.info("Para:      {}", recipient);
        log.info("Asunto:    {}", payload.title());
        log.info("Tipo:      {}", payload.type());
        log.info("Cuerpo:    {}", payload.message());
        log.info("Link:      https://grandprixtracker.com{}", payload.destinationUrl() != null ? payload.destinationUrl() : "");
        log.info("Metadata:  {}", payload.metadata());
        log.info("=================================================================");
    }
}
