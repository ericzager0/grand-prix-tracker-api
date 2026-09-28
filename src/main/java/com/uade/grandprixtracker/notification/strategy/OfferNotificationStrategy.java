package com.uade.grandprixtracker.notification.strategy;

import com.uade.grandprixtracker.notification.event.OfferPublishedEvent;
import com.uade.grandprixtracker.notification.model.NotificationType;
import org.springframework.stereotype.Component;

/**
 * Estrategia para procesar eventos de ofertas y promociones.
 */
@Component
public class OfferNotificationStrategy implements NotificationContentStrategy<OfferPublishedEvent> {

    @Override
    public boolean supports(Object event) {
        return event instanceof OfferPublishedEvent;
    }

    @Override
    public NotificationPayload createPayload(OfferPublishedEvent event) {
        String discountText = event.discountPercentage() != null && event.discountPercentage() > 0
                ? " (" + event.discountPercentage() + "% OFF)"
                : "";

        String title = "🔥 Nueva Oferta: " + event.title();
        String message = event.description() + discountText;
        String metadata = "{\"offerId\":\"" + event.offerId() + "\",\"discount\":" + event.discountPercentage() + "}";

        return new NotificationPayload(
                event.targetUserId(),
                title,
                message,
                NotificationType.OFFER,
                event.destinationUrl() != null ? event.destinationUrl() : "/offers",
                metadata,
                com.uade.grandprixtracker.notification.model.DeliveryMode.PUB_SUB
        );
    }
}
