package com.uade.grandprixtracker.notification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.uade.grandprixtracker.notification.dto.CreateNotificationRequestDto;
import com.uade.grandprixtracker.notification.dto.NotificationResponseDto;
import com.uade.grandprixtracker.notification.dto.UnreadCountResponseDto;
import com.uade.grandprixtracker.notification.model.NotificationType;
import com.uade.grandprixtracker.notification.service.NotificationService;
import com.uade.grandprixtracker.shared.exception.GlobalExceptionHandler;
import com.uade.grandprixtracker.shared.exception.ResourceNotFoundException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private UUID notificationId;
    private UUID userId;
    private NotificationResponseDto sampleDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        notificationId = UUID.randomUUID();
        userId = UUID.randomUUID();

        sampleDto = new NotificationResponseDto(
                notificationId,
                userId,
                "Pedido Confirmado",
                "Tu reserva para Monza está confirmada",
                NotificationType.ORDER_CONFIRMATION,
                false,
                "/pedidos/1",
                null,
                OffsetDateTime.now()
        );
    }

    @Test
    void getNotifications_returnsList() throws Exception {
        when(notificationService.getNotifications(userId, false)).thenReturn(List.of(sampleDto));

        mockMvc.perform(get("/notifications")
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].titulo").value("Pedido Confirmado"))
                .andExpect(jsonPath("$.data[0].tipo").value("ORDER_CONFIRMATION"));

        verify(notificationService, times(1)).getNotifications(userId, false);
    }

    @Test
    void getUnreadCount_returnsCount() throws Exception {
        when(notificationService.getUnreadCount(userId)).thenReturn(new UnreadCountResponseDto(5L));

        mockMvc.perform(get("/notifications/unread-count")
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.unreadCount").value(5));

        verify(notificationService, times(1)).getUnreadCount(userId);
    }

    @Test
    void createNotification_returnsCreated() throws Exception {
        when(notificationService.createNotification(any(CreateNotificationRequestDto.class))).thenReturn(sampleDto);

        String jsonBody = """
                {
                    "idUsuario": "%s",
                    "titulo": "Pedido Confirmado",
                    "mensaje": "Tu reserva para Monza está confirmada",
                    "tipo": "ORDER_CONFIRMATION",
                    "urlDestino": "/pedidos/1"
                }
                """.formatted(userId);

        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.titulo").value("Pedido Confirmado"));

        verify(notificationService, times(1)).createNotification(any(CreateNotificationRequestDto.class));
    }

    @Test
    void markAsRead_returnsUpdated() throws Exception {
        NotificationResponseDto updatedDto = new NotificationResponseDto(
                notificationId,
                userId,
                "Pedido Confirmado",
                "Tu reserva para Monza está confirmada",
                NotificationType.ORDER_CONFIRMATION,
                true,
                "/pedidos/1",
                null,
                OffsetDateTime.now()
        );

        when(notificationService.markAsRead(notificationId)).thenReturn(updatedDto);

        mockMvc.perform(patch("/notifications/" + notificationId + "/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.leido").value(true));

        verify(notificationService, times(1)).markAsRead(notificationId);
    }

    @Test
    void markAsRead_notFound_returns404() throws Exception {
        when(notificationService.markAsRead(notificationId))
                .thenThrow(new ResourceNotFoundException("Notificación", "id", notificationId));

        mockMvc.perform(patch("/notifications/" + notificationId + "/read"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void markAllAsRead_returnsSuccess() throws Exception {
        doNothing().when(notificationService).markAllAsRead(userId);

        mockMvc.perform(patch("/notifications/read-all")
                        .param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(notificationService, times(1)).markAllAsRead(userId);
    }
}
