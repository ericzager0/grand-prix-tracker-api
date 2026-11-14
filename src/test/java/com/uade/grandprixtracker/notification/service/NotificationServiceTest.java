package com.uade.grandprixtracker.notification.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.uade.grandprixtracker.notification.dto.CreateNotificationRequestDto;
import com.uade.grandprixtracker.notification.dto.NotificationResponseDto;
import com.uade.grandprixtracker.notification.dto.UnreadCountResponseDto;
import com.uade.grandprixtracker.notification.model.Notification;
import com.uade.grandprixtracker.notification.model.NotificationType;
import com.uade.grandprixtracker.notification.repository.NotificationRepository;
import com.uade.grandprixtracker.shared.exception.ResourceNotFoundException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private Notification testNotification;
    private UUID notificationId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        notificationId = UUID.randomUUID();
        userId = UUID.randomUUID();

        testNotification = new Notification(
                userId,
                "Confirmación de Pedido",
                "Tu reserva para el GP de Monza fue confirmada.",
                NotificationType.ORDER_CONFIRMATION,
                "/pedidos/123",
                "{\"orderId\":\"123\"}"
        );
        testNotification.setIdNotificacion(notificationId);
        testNotification.setCreadoEn(OffsetDateTime.now());
    }

    @Test
    void getNotifications_all_returnsList() {
        when(notificationRepository.findAllForUser(userId)).thenReturn(List.of(testNotification));

        List<NotificationResponseDto> result = notificationService.getNotifications(userId, false);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Confirmación de Pedido", result.getFirst().titulo());
        assertEquals(NotificationType.ORDER_CONFIRMATION, result.getFirst().tipo());
        verify(notificationRepository, times(1)).findAllForUser(userId);
    }

    @Test
    void getNotifications_unreadOnly_returnsFiltered() {
        when(notificationRepository.findUnreadForUser(userId)).thenReturn(List.of(testNotification));

        List<NotificationResponseDto> result = notificationService.getNotifications(userId, true);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(notificationRepository, times(1)).findUnreadForUser(userId);
    }

    @Test
    void getUnreadCount_returnsCount() {
        when(notificationRepository.countUnreadForUser(userId)).thenReturn(3L);

        UnreadCountResponseDto result = notificationService.getUnreadCount(userId);

        assertNotNull(result);
        assertEquals(3L, result.unreadCount());
        verify(notificationRepository, times(1)).countUnreadForUser(userId);
    }

    @Test
    void createNotification_success() {
        CreateNotificationRequestDto dto = new CreateNotificationRequestDto(
                userId,
                "Oferta Flash",
                "20% off en traslados",
                NotificationType.OFFER,
                "/servicios",
                null
        );

        Notification saved = new Notification(userId, "Oferta Flash", "20% off en traslados", NotificationType.OFFER, "/servicios", null);
        saved.setIdNotificacion(UUID.randomUUID());
        saved.setCreadoEn(OffsetDateTime.now());

        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        NotificationResponseDto result = notificationService.createNotification(dto);

        assertNotNull(result);
        assertEquals("Oferta Flash", result.titulo());
        assertEquals(NotificationType.OFFER, result.tipo());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void createForUser_usaElUsuarioRecibidoYNoElDelDto() {
        UUID otroUsuario = UUID.randomUUID();
        CreateNotificationRequestDto dto = new CreateNotificationRequestDto(
                otroUsuario, "Compra confirmada", "Detalle", NotificationType.ORDER_CONFIRMATION, "/profile", null);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponseDto result = notificationService.createForUser(userId, dto);

        assertEquals(userId, result.idUsuario());
        org.mockito.ArgumentCaptor<Notification> guardada = org.mockito.ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(guardada.capture());
        assertEquals(userId, guardada.getValue().getIdUsuario());
    }

    @Test
    void createForUser_sinUsuario_noCreaUnaGlobal() {
        CreateNotificationRequestDto dto = new CreateNotificationRequestDto(
                null, "Oferta", "Detalle", NotificationType.OFFER, "/calendar", null);

        assertThrows(IllegalArgumentException.class, () -> notificationService.createForUser(null, dto));
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAsRead_propia_laMarca() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        NotificationResponseDto result = notificationService.markAsRead(userId, notificationId);

        assertTrue(result.leido());
        verify(notificationRepository, times(1)).save(testNotification);
    }

    @Test
    void markAsRead_deOtroUsuario_respondeComoInexistenteYNoLaModifica() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(testNotification));

        assertThrows(ResourceNotFoundException.class,
                () -> notificationService.markAsRead(UUID.randomUUID(), notificationId));

        assertFalse(testNotification.getLeido());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAsRead_global_noSeModificaParaElResto() {
        Notification global = new Notification(null, "Oferta", "20% off", NotificationType.OFFER, "/calendar", null);
        global.setIdNotificacion(notificationId);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(global));

        NotificationResponseDto result = notificationService.markAsRead(userId, notificationId);

        assertFalse(result.leido());
        assertFalse(global.getLeido());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAsRead_notFound_throwsException() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> notificationService.markAsRead(userId, notificationId));
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAllAsRead_callsRepository() {
        notificationService.markAllAsRead(userId);
        verify(notificationRepository, times(1)).markAllAsReadForUser(userId);
    }
}
