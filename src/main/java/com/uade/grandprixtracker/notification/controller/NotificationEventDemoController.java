package com.uade.grandprixtracker.notification.controller;

import com.uade.grandprixtracker.notification.dto.SimulateBookingEventDto;
import com.uade.grandprixtracker.notification.dto.SimulateOfferEventDto;
import com.uade.grandprixtracker.notification.dto.SimulatePurchaseStatusEventDto;
import com.uade.grandprixtracker.notification.event.BookingConfirmedEvent;
import com.uade.grandprixtracker.notification.event.OfferPublishedEvent;
import com.uade.grandprixtracker.notification.event.PurchaseStatusChangedEvent;
import com.uade.grandprixtracker.shared.response.ApiResponse;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador de demostración y prueba para disparar eventos de dominio.
 * Permite validar el funcionamiento del patrón Observer (Spring Events) y las estrategias de notificación.
 */
@RestController
@RequestMapping("/notifications/events")
public class NotificationEventDemoController {

    private final ApplicationEventPublisher eventPublisher;

    public NotificationEventDemoController(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Dispara un evento de publicación de oferta.
     */
    @PostMapping("/offer")
    public ResponseEntity<ApiResponse<String>> triggerOfferEvent(@RequestBody SimulateOfferEventDto dto) {
        OfferPublishedEvent event = new OfferPublishedEvent(
                UUID.randomUUID(),
                dto.title() != null ? dto.title() : "Descuento Flash en Paddock Club",
                dto.description() != null ? dto.description() : "Aprovechá hasta agotar stock para el próximo Gran Premio",
                dto.discountPercentage() != null ? dto.discountPercentage() : 25,
                dto.targetUserId(),
                dto.destinationUrl() != null ? dto.destinationUrl() : "/offers/flash-sale"
        );

        eventPublisher.publishEvent(event);
        return ResponseEntity.ok(ApiResponse.success(
                "Evento OfferPublishedEvent publicado con éxito. Los observadores fueron notificados.",
                "Oferta ID: " + event.offerId()
        ));
    }

    /**
     * Dispara un evento de confirmación de reserva de entradas.
     */
    @PostMapping("/booking-confirmed")
    public ResponseEntity<ApiResponse<String>> triggerBookingEvent(@RequestBody SimulateBookingEventDto dto) {
        UUID bookingId = UUID.randomUUID();
        BookingConfirmedEvent event = new BookingConfirmedEvent(
                bookingId,
                dto.userId() != null ? dto.userId() : UUID.randomUUID(),
                dto.grandPrixName() != null ? dto.grandPrixName() : "Gran Premio de Monza",
                dto.seatsCount() != null ? dto.seatsCount() : 2,
                dto.totalAmount()
        );

        eventPublisher.publishEvent(event);
        return ResponseEntity.ok(ApiResponse.success(
                "Evento BookingConfirmedEvent publicado con éxito. Los observadores fueron notificados.",
                "Reserva ID: " + bookingId
        ));
    }

    /**
     * Dispara un evento de cambio de estado de compra.
     */
    @PostMapping("/purchase-status")
    public ResponseEntity<ApiResponse<String>> triggerPurchaseStatusEvent(@RequestBody SimulatePurchaseStatusEventDto dto) {
        UUID purchaseId = UUID.randomUUID();
        PurchaseStatusChangedEvent event = new PurchaseStatusChangedEvent(
                purchaseId,
                dto.userId() != null ? dto.userId() : UUID.randomUUID(),
                dto.productName() != null ? dto.productName() : "Entrada Tribuna Silver",
                dto.previousStatus(),
                dto.newStatus(),
                dto.totalPrice()
        );

        eventPublisher.publishEvent(event);
        return ResponseEntity.ok(ApiResponse.success(
                "Evento PurchaseStatusChangedEvent publicado con éxito. Los observadores fueron notificados.",
                "Compra ID: " + purchaseId
        ));
    }
}
