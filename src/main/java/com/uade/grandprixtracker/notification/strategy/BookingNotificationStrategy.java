package com.uade.grandprixtracker.notification.strategy;

import com.uade.grandprixtracker.notification.event.BookingConfirmedEvent;
import com.uade.grandprixtracker.notification.model.NotificationType;
import org.springframework.stereotype.Component;

/**
 * Estrategia para procesar confirmaciones de reservas de entradas/eventos.
 */
@Component
public class BookingNotificationStrategy implements NotificationContentStrategy<BookingConfirmedEvent> {

    @Override
    public boolean supports(Object event) {
        return event instanceof BookingConfirmedEvent;
    }

    @Override
    public NotificationPayload createPayload(BookingConfirmedEvent event) {
        String title = "✅ Reserva Confirmada: " + event.grandPrixName();
        String message = String.format(
                "¡Tu reserva de %d entrada(s) para %s fue confirmada con éxito por un total de $%s!",
                event.seatsCount() != null ? event.seatsCount() : 1,
                event.grandPrixName(),
                event.totalAmount() != null ? event.totalAmount().toPlainString() : "0.00"
        );
        String destinationUrl = "/bookings/" + (event.bookingId() != null ? event.bookingId().toString() : "");
        String metadata = "{\"bookingId\":\"" + event.bookingId() + "\",\"event\":\"" + event.grandPrixName() + "\"}";

        return new NotificationPayload(
                event.userId(),
                title,
                message,
                NotificationType.ORDER_CONFIRMATION,
                destinationUrl,
                metadata,
                com.uade.grandprixtracker.notification.model.DeliveryMode.POINT_TO_POINT
        );
    }
}
