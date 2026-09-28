package com.uade.grandprixtracker.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.uade.grandprixtracker.notification.channel.InAppNotificationChannel;
import com.uade.grandprixtracker.notification.channel.NotificationChannel;
import com.uade.grandprixtracker.notification.dispatcher.NotificationDispatcher;
import com.uade.grandprixtracker.notification.dto.CreateNotificationRequestDto;
import com.uade.grandprixtracker.notification.event.BookingConfirmedEvent;
import com.uade.grandprixtracker.notification.event.OfferPublishedEvent;
import com.uade.grandprixtracker.notification.event.PurchaseStatus;
import com.uade.grandprixtracker.notification.event.PurchaseStatusChangedEvent;
import com.uade.grandprixtracker.notification.listener.NotificationEventListener;
import com.uade.grandprixtracker.notification.model.NotificationType;
import com.uade.grandprixtracker.notification.service.NotificationService;
import com.uade.grandprixtracker.notification.strategy.BookingNotificationStrategy;
import com.uade.grandprixtracker.notification.strategy.NotificationPayload;
import com.uade.grandprixtracker.notification.strategy.OfferNotificationStrategy;
import com.uade.grandprixtracker.notification.strategy.PurchaseStatusNotificationStrategy;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NotificationPatternTest {

    private NotificationService notificationService;
    private InAppNotificationChannel inAppChannel;
    private NotificationDispatcher dispatcher;
    private NotificationEventListener listener;

    private OfferNotificationStrategy offerStrategy;
    private BookingNotificationStrategy bookingStrategy;
    private PurchaseStatusNotificationStrategy purchaseStrategy;

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        inAppChannel = new InAppNotificationChannel(notificationService);

        offerStrategy = new OfferNotificationStrategy();
        bookingStrategy = new BookingNotificationStrategy();
        purchaseStrategy = new PurchaseStatusNotificationStrategy();

        List<NotificationChannel> channels = List.of(inAppChannel);
        dispatcher = new NotificationDispatcher(
                List.of(offerStrategy, bookingStrategy, purchaseStrategy),
                channels
        );

        listener = new NotificationEventListener(dispatcher);
    }

    @Test
    @DisplayName("Observer & Strategy: Debería procesar OfferPublishedEvent y persistir en el canal In-App")
    void testOfferEventFlow() {
        UUID offerId = UUID.randomUUID();
        OfferPublishedEvent event = new OfferPublishedEvent(
                offerId,
                "Entradas GP Silverstone",
                "Descuento especial fin de semana",
                20,
                null,
                "/offers/silverstone"
        );

        listener.onOfferPublished(event);

        ArgumentCaptor<CreateNotificationRequestDto> captor = ArgumentCaptor.forClass(CreateNotificationRequestDto.class);
        verify(notificationService).createNotification(captor.capture());

        CreateNotificationRequestDto captured = captor.getValue();
        assertEquals(NotificationType.OFFER, captured.tipo());
        assertTrue(captured.titulo().contains("GP Silverstone"));
        assertTrue(captured.mensaje().contains("20% OFF"));
    }

    @Test
    @DisplayName("Observer & Strategy: Debería procesar BookingConfirmedEvent con tipo ORDER_CONFIRMATION")
    void testBookingEventFlow() {
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        BookingConfirmedEvent event = new BookingConfirmedEvent(
                bookingId,
                userId,
                "GP de Interlagos",
                2,
                new BigDecimal("500.00")
        );

        listener.onBookingConfirmed(event);

        ArgumentCaptor<CreateNotificationRequestDto> captor = ArgumentCaptor.forClass(CreateNotificationRequestDto.class);
        verify(notificationService).createNotification(captor.capture());

        CreateNotificationRequestDto captured = captor.getValue();
        assertEquals(userId, captured.idUsuario());
        assertEquals(NotificationType.ORDER_CONFIRMATION, captured.tipo());
        assertTrue(captured.titulo().contains("Interlagos"));
    }

    @Test
    @DisplayName("Observer & Strategy: Debería procesar PurchaseStatusChangedEvent reflejando estado APPROVED")
    void testPurchaseStatusEventFlow() {
        UUID userId = UUID.randomUUID();
        UUID purchaseId = UUID.randomUUID();
        PurchaseStatusChangedEvent event = new PurchaseStatusChangedEvent(
                purchaseId,
                userId,
                "Remera Ferrari F1",
                PurchaseStatus.PENDING,
                PurchaseStatus.APPROVED,
                new BigDecimal("80.00")
        );

        listener.onPurchaseStatusChanged(event);

        ArgumentCaptor<CreateNotificationRequestDto> captor = ArgumentCaptor.forClass(CreateNotificationRequestDto.class);
        verify(notificationService).createNotification(captor.capture());

        CreateNotificationRequestDto captured = captor.getValue();
        assertEquals(userId, captured.idUsuario());
        assertEquals(NotificationType.ORDER_CONFIRMATION, captured.tipo());
        assertTrue(captured.titulo().contains("Aprobado"));
        assertTrue(captured.mensaje().contains("Remera Ferrari F1"));
    }
}
