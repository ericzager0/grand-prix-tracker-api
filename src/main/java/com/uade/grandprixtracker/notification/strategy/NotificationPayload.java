package com.uade.grandprixtracker.notification.strategy;

import com.uade.grandprixtracker.notification.model.DeliveryMode;
import com.uade.grandprixtracker.notification.model.NotificationType;
import java.util.UUID;

/**
 * Objeto genérico intermedio que encapsula los datos listos para ser despachados a cualquier canal.
 */
public record NotificationPayload(
        UUID userId,
        String title,
        String message,
        NotificationType type,
        String destinationUrl,
        String metadata,
        DeliveryMode deliveryMode
) {
    public NotificationPayload(
            UUID userId,
            String title,
            String message,
            NotificationType type,
            String destinationUrl,
            String metadata
    ) {
        this(
                userId,
                title,
                message,
                type,
                destinationUrl,
                metadata,
                userId == null ? DeliveryMode.PUB_SUB : DeliveryMode.POINT_TO_POINT
        );
    }

    public boolean isPubSub() {
        return deliveryMode == DeliveryMode.PUB_SUB;
    }

    public boolean isPointToPoint() {
        return deliveryMode == DeliveryMode.POINT_TO_POINT;
    }
}
