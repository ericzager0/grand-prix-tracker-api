package com.uade.grandprixtracker.notification.strategy;

import com.uade.grandprixtracker.notification.event.PurchaseStatus;
import com.uade.grandprixtracker.notification.event.PurchaseStatusChangedEvent;
import com.uade.grandprixtracker.notification.model.NotificationType;
import org.springframework.stereotype.Component;

/**
 * Estrategia para procesar transiciones de estado en compras de merchandising/tickets.
 */
@Component
public class PurchaseStatusNotificationStrategy implements NotificationContentStrategy<PurchaseStatusChangedEvent> {

    @Override
    public boolean supports(Object event) {
        return event instanceof PurchaseStatusChangedEvent;
    }

    @Override
    public NotificationPayload createPayload(PurchaseStatusChangedEvent event) {
        PurchaseStatus status = event.newStatus();
        String title;
        String message;
        NotificationType type = NotificationType.ORDER_CONFIRMATION;

        switch (status) {
            case APPROVED -> {
                title = "💳 Pago Aprobado - Compra #" + shortId(event.purchaseId());
                message = String.format("El pago por '%s' fue acreditado correctamente.", event.productName());
            }
            case PROCESSING -> {
                title = "⏳ Preparando tu pedido - Compra #" + shortId(event.purchaseId());
                message = String.format("Estamos procesando tu orden de '%s'.", event.productName());
            }
            case SHIPPED -> {
                title = "🚚 Pedido en camino - Compra #" + shortId(event.purchaseId());
                message = String.format("Tu producto '%s' ya está en viaje.", event.productName());
            }
            case DELIVERED -> {
                title = "🎉 Pedido Entregado - Compra #" + shortId(event.purchaseId());
                message = String.format("¡Tu orden de '%s' ha sido entregada!", event.productName());
            }
            case CANCELLED -> {
                title = "❌ Compra Cancelada - Compra #" + shortId(event.purchaseId());
                message = String.format("Tu compra de '%s' ha sido cancelada.", event.productName());
                type = NotificationType.SYSTEM;
            }
            case REFUNDED -> {
                title = "💰 Reembolso Procesado - Compra #" + shortId(event.purchaseId());
                message = String.format("Se emitió el reembolso correspondiente para '%s'.", event.productName());
                type = NotificationType.SYSTEM;
            }
            default -> {
                title = "📦 Actualización de compra #" + shortId(event.purchaseId());
                message = String.format("Tu compra de '%s' cambió al estado %s.", event.productName(), status);
            }
        }

        String destinationUrl = "/purchases/" + (event.purchaseId() != null ? event.purchaseId().toString() : "");
        String metadata = String.format(
                "{\"purchaseId\":\"%s\",\"previousStatus\":\"%s\",\"newStatus\":\"%s\"}",
                event.purchaseId(),
                event.previousStatus(),
                event.newStatus()
        );

        return new NotificationPayload(
                event.userId(),
                title,
                message,
                type,
                destinationUrl,
                metadata,
                com.uade.grandprixtracker.notification.model.DeliveryMode.POINT_TO_POINT
        );
    }

    private String shortId(java.util.UUID id) {
        if (id == null) {
            return "";
        }
        return id.toString().substring(0, 8);
    }
}
