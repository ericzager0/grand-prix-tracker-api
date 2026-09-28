package com.uade.grandprixtracker.notification.channel;

import com.uade.grandprixtracker.notification.dto.CreateNotificationRequestDto;
import com.uade.grandprixtracker.notification.service.NotificationService;
import com.uade.grandprixtracker.notification.strategy.NotificationPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Canal de notificación In-App: persiste la notificación en la base de datos
 * para que el usuario pueda consultarla desde la aplicación web o móvil.
 */
@Component
public class InAppNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(InAppNotificationChannel.class);
    private final NotificationService notificationService;

    public InAppNotificationChannel(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public String getChannelName() {
        return "IN_APP_DATABASE";
    }

    @Override
    public void send(NotificationPayload payload) {
        CreateNotificationRequestDto requestDto = new CreateNotificationRequestDto(
                payload.userId(),
                payload.title(),
                payload.message(),
                payload.type(),
                payload.destinationUrl(),
                payload.metadata()
        );

        notificationService.createNotification(requestDto);
        if (payload.isPubSub()) {
            log.info("[CANAL IN-APP - PUB/SUB] Notificación broadcast persistida para TODOS los usuarios: '{}'", payload.title());
        } else {
            log.info("[CANAL IN-APP - PUNTO A PUNTO] Notificación individual guardada en BD para usuario={}: '{}'",
                    payload.userId(), payload.title());
        }
    }
}
