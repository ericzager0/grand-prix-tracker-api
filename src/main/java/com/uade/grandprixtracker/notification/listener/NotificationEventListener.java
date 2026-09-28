package com.uade.grandprixtracker.notification.listener;

import com.uade.grandprixtracker.notification.dispatcher.NotificationDispatcher;
import com.uade.grandprixtracker.notification.event.BookingConfirmedEvent;
import com.uade.grandprixtracker.notification.event.OfferPublishedEvent;
import com.uade.grandprixtracker.notification.event.PurchaseStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Observador (Observer) que escucha los eventos de dominio de la aplicación.
 * Implementa el rol de Concrete Observer del patrón Observer mediante los Spring Application Events.
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);
    private final NotificationDispatcher notificationDispatcher;

    public NotificationEventListener(NotificationDispatcher notificationDispatcher) {
        this.notificationDispatcher = notificationDispatcher;
    }

    /**
     * Reacciona cuando se publica una nueva oferta comercial o descuento.
     */
    @EventListener
    public void onOfferPublished(OfferPublishedEvent event) {
        log.info("[OBSERVER] Evento OfferPublishedEvent capturado: '{}'", event.title());
        notificationDispatcher.dispatch(event);
    }

    /**
     * Reacciona cuando se confirma la reserva de un evento de Gran Premio.
     */
    @EventListener
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        log.info("[OBSERVER] Evento BookingConfirmedEvent capturado para reserva={}", event.bookingId());
        notificationDispatcher.dispatch(event);
    }

    /**
     * Reacciona cuando una compra cambia de estado (aprobada, despachada, cancelada, etc.).
     */
    @EventListener
    public void onPurchaseStatusChanged(PurchaseStatusChangedEvent event) {
        log.info("[OBSERVER] Evento PurchaseStatusChangedEvent capturado para compra={}, nuevoEstado={}",
                event.purchaseId(), event.newStatus());
        notificationDispatcher.dispatch(event);
    }
}
