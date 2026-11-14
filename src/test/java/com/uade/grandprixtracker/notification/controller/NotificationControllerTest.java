package com.uade.grandprixtracker.notification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.uade.grandprixtracker.auth.config.SecurityConfig;
import com.uade.grandprixtracker.notification.dto.CreateNotificationRequestDto;
import com.uade.grandprixtracker.notification.dto.NotificationResponseDto;
import com.uade.grandprixtracker.notification.dto.UnreadCountResponseDto;
import com.uade.grandprixtracker.notification.model.NotificationType;
import com.uade.grandprixtracker.notification.service.NotificationService;
import com.uade.grandprixtracker.shared.exception.ResourceNotFoundException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Pasa por la cadena de filtros de SecurityConfig; el JwtDecoder es un mock y el usuario se simula con jwt().
@WebMvcTest(NotificationController.class)
@Import(SecurityConfig.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private NotificationService notificationService;

    private UUID notificationId;
    private UUID userId;
    private UUID otroUsuario;
    private NotificationResponseDto sampleDto;

    @BeforeEach
    void setUp() {
        notificationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        otroUsuario = UUID.randomUUID();

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

    // Usuario autenticado con Supabase: el claim "sub" es el id del usuario.
    private JwtRequestPostProcessor usuario() {
        return jwt().jwt(j -> j.subject(userId.toString()));
    }

    private String body(String urlDestino) {
        return """
                {
                    "idUsuario": "%s",
                    "titulo": "Pedido Confirmado",
                    "mensaje": "Tu reserva para Monza está confirmada",
                    "tipo": "ORDER_CONFIRMATION",
                    "urlDestino": "%s",
                    "localOnly": false
                }
                """.formatted(otroUsuario, urlDestino);
    }

    @Test
    void getNotifications_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/notifications").param("userId", userId.toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(notificationService);
    }

    @Test
    void getUnreadCount_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/notifications/unread-count"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(notificationService);
    }

    @Test
    void createNotification_sinToken_devuelve401() throws Exception {
        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("/pedidos/1")))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(notificationService);
    }

    @Test
    void markAsRead_sinToken_devuelve401() throws Exception {
        mockMvc.perform(patch("/notifications/" + notificationId + "/read"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(notificationService);
    }

    @Test
    void markAllAsRead_sinToken_devuelve401() throws Exception {
        mockMvc.perform(patch("/notifications/read-all").param("userId", userId.toString()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(notificationService);
    }

    @Test
    void getNotifications_usaElSubDelTokenEIgnoraElUserIdDeLaUrl() throws Exception {
        when(notificationService.getNotifications(userId, false)).thenReturn(List.of(sampleDto));

        mockMvc.perform(get("/notifications")
                        .with(usuario())
                        .param("userId", otroUsuario.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].titulo").value("Pedido Confirmado"))
                .andExpect(jsonPath("$.data[0].tipo").value("ORDER_CONFIRMATION"));

        verify(notificationService).getNotifications(userId, false);
        verify(notificationService, never()).getNotifications(eq(otroUsuario), any());
    }

    @Test
    void getUnreadCount_usaElSubDelToken() throws Exception {
        when(notificationService.getUnreadCount(userId)).thenReturn(new UnreadCountResponseDto(5L));

        mockMvc.perform(get("/notifications/unread-count")
                        .with(usuario())
                        .param("userId", otroUsuario.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(5));

        verify(notificationService).getUnreadCount(userId);
    }

    @Test
    void createNotification_laCreaParaElUsuarioDelTokenAunqueElBodyDigaOtro() throws Exception {
        when(notificationService.createForUser(eq(userId), any(CreateNotificationRequestDto.class))).thenReturn(sampleDto);

        mockMvc.perform(post("/notifications")
                        .with(usuario())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("/profile?tab=reservas")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.titulo").value("Pedido Confirmado"));

        ArgumentCaptor<CreateNotificationRequestDto> dto = ArgumentCaptor.forClass(CreateNotificationRequestDto.class);
        verify(notificationService).createForUser(eq(userId), dto.capture());
        org.junit.jupiter.api.Assertions.assertEquals("/profile?tab=reservas", dto.getValue().urlDestino());
        verify(notificationService, never()).createNotification(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://sitio-malicioso.example/login",
            "//sitio-malicioso.example",
            "/\\\\sitio-malicioso.example",
            "javascript:alert(1)",
            "pedidos/1"
    })
    void createNotification_urlDestinoExterna_devuelve400(String urlDestino) throws Exception {
        mockMvc.perform(post("/notifications")
                        .with(usuario())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(urlDestino)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(notificationService);
    }

    @Test
    void createNotification_sinTitulo_devuelve400() throws Exception {
        mockMvc.perform(post("/notifications")
                        .with(usuario())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mensaje\": \"hola\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(notificationService);
    }

    @Test
    void createNotification_tituloDemasiadoLargo_devuelve400() throws Exception {
        String json = "{\"titulo\": \"%s\", \"mensaje\": \"hola\"}".formatted("a".repeat(151));

        mockMvc.perform(post("/notifications")
                        .with(usuario())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(notificationService);
    }

    @Test
    void markAsRead_pasaElUsuarioDelToken() throws Exception {
        NotificationResponseDto updatedDto = new NotificationResponseDto(
                notificationId, userId, "Pedido Confirmado", "Tu reserva para Monza está confirmada",
                NotificationType.ORDER_CONFIRMATION, true, "/pedidos/1", null, OffsetDateTime.now());
        when(notificationService.markAsRead(userId, notificationId)).thenReturn(updatedDto);

        mockMvc.perform(patch("/notifications/" + notificationId + "/read").with(usuario()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.leido").value(true));

        verify(notificationService).markAsRead(userId, notificationId);
    }

    @Test
    void markAsRead_deOtroUsuarioOInexistente_devuelve404() throws Exception {
        when(notificationService.markAsRead(userId, notificationId))
                .thenThrow(new ResourceNotFoundException("Notificación no encontrada"));

        mockMvc.perform(patch("/notifications/" + notificationId + "/read").with(usuario()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void markAllAsRead_usaElSubDelTokenEIgnoraElUserIdDeLaUrl() throws Exception {
        mockMvc.perform(patch("/notifications/read-all")
                        .with(usuario())
                        .param("userId", otroUsuario.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(notificationService).markAllAsRead(userId);
        verify(notificationService, never()).markAllAsRead(otroUsuario);
    }

    // El controller de demo solo se registra con perfil dev/local: acá (sin perfil) no existe.
    @Test
    void endpointsDeDemo_sinPerfilDev_noExisten() throws Exception {
        mockMvc.perform(post("/notifications/events/offer")
                        .with(usuario())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(notificationService);
    }
}
