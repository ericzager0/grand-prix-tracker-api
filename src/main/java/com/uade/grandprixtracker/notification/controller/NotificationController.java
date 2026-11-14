package com.uade.grandprixtracker.notification.controller;

import com.uade.grandprixtracker.notification.dto.CreateNotificationRequestDto;
import com.uade.grandprixtracker.notification.dto.NotificationResponseDto;
import com.uade.grandprixtracker.notification.dto.UnreadCountResponseDto;
import com.uade.grandprixtracker.notification.service.NotificationService;
import com.uade.grandprixtracker.shared.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Todos los endpoints requieren token. El usuario sale siempre del claim "sub" del JWT:
// cualquier ?userId= o idUsuario que mande el cliente se ignora, para que nadie pueda
// leer, crear o modificar notificaciones de otro usuario.
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponseDto>>> getNotifications(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "unreadOnly", defaultValue = "false") Boolean unreadOnly) {
        List<NotificationResponseDto> notifications = notificationService.getNotifications(idUsuario(jwt), unreadOnly);
        return ResponseEntity.ok(ApiResponse.success("Notificaciones obtenidas correctamente", notifications));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponseDto>> getUnreadCount(
            @AuthenticationPrincipal Jwt jwt) {
        UnreadCountResponseDto countDto = notificationService.getUnreadCount(idUsuario(jwt));
        return ResponseEntity.ok(ApiResponse.success("Conteo de no leídas obtenido correctamente", countDto));
    }

    // Un cliente solo puede crear notificaciones para sí mismo, nunca globales ni para otro usuario.
    @PostMapping
    public ResponseEntity<ApiResponse<NotificationResponseDto>> createNotification(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateNotificationRequestDto dto) {
        NotificationResponseDto created = notificationService.createForUser(idUsuario(jwt), dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Notificación creada correctamente", created));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponseDto>> markAsRead(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        NotificationResponseDto updated = notificationService.markAsRead(idUsuario(jwt), id);
        return ResponseEntity.ok(ApiResponse.success("Notificación marcada como leída", updated));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal Jwt jwt) {
        notificationService.markAllAsRead(idUsuario(jwt));
        return ResponseEntity.ok(ApiResponse.success("Todas las notificaciones fueron marcadas como leídas", null));
    }

    // El claim "sub" del JWT de Supabase es el id del usuario.
    private static UUID idUsuario(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
