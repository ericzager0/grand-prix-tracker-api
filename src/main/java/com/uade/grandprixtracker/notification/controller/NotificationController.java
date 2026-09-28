package com.uade.grandprixtracker.notification.controller;

import com.uade.grandprixtracker.notification.dto.CreateNotificationRequestDto;
import com.uade.grandprixtracker.notification.dto.NotificationResponseDto;
import com.uade.grandprixtracker.notification.dto.UnreadCountResponseDto;
import com.uade.grandprixtracker.notification.service.NotificationService;
import com.uade.grandprixtracker.shared.response.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponseDto>>> getNotifications(
            @RequestParam(name = "userId", required = false) UUID userId,
            @RequestParam(name = "unreadOnly", defaultValue = "false") Boolean unreadOnly) {
        List<NotificationResponseDto> notifications = notificationService.getNotifications(userId, unreadOnly);
        return ResponseEntity.ok(ApiResponse.success("Notificaciones obtenidas correctamente", notifications));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponseDto>> getUnreadCount(
            @RequestParam(name = "userId", required = false) UUID userId) {
        UnreadCountResponseDto countDto = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.success("Conteo de no leídas obtenido correctamente", countDto));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<NotificationResponseDto>> createNotification(
            @RequestBody CreateNotificationRequestDto dto) {
        NotificationResponseDto created = notificationService.createNotification(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Notificación creada correctamente", created));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponseDto>> markAsRead(
            @PathVariable UUID id) {
        NotificationResponseDto updated = notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("Notificación marcada como leída", updated));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @RequestParam(name = "userId") UUID userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success("Todas las notificaciones fueron marcadas como leídas", null));
    }
}
