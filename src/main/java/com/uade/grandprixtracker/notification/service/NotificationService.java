package com.uade.grandprixtracker.notification.service;

import com.uade.grandprixtracker.notification.dto.CreateNotificationRequestDto;
import com.uade.grandprixtracker.notification.dto.NotificationResponseDto;
import com.uade.grandprixtracker.notification.dto.UnreadCountResponseDto;
import com.uade.grandprixtracker.notification.model.Notification;
import com.uade.grandprixtracker.notification.model.NotificationType;
import com.uade.grandprixtracker.notification.repository.NotificationRepository;
import com.uade.grandprixtracker.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<NotificationResponseDto> getNotifications(UUID userId, Boolean unreadOnly) {
        List<Notification> notifications;
        if (Boolean.TRUE.equals(unreadOnly)) {
            notifications = notificationRepository.findUnreadForUser(userId);
        } else {
            notifications = notificationRepository.findAllForUser(userId);
        }
        return notifications.stream()
                .map(this::mapToDto)
                .toList();
    }

    public UnreadCountResponseDto getUnreadCount(UUID userId) {
        long count = notificationRepository.countUnreadForUser(userId);
        return new UnreadCountResponseDto(count);
    }

    @Transactional
    public NotificationResponseDto createNotification(CreateNotificationRequestDto dto) {
        Notification notification = new Notification(
                dto.idUsuario(),
                dto.titulo(),
                dto.mensaje(),
                dto.tipo() != null ? dto.tipo() : NotificationType.SYSTEM,
                dto.urlDestino(),
                dto.metadata()
        );
        Notification saved = notificationRepository.save(notification);
        return mapToDto(saved);
    }

    @Transactional
    public NotificationResponseDto markAsRead(UUID idNotificacion) {
        Notification notification = notificationRepository.findById(idNotificacion)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación", "id", idNotificacion));
        notification.setLeido(true);
        Notification updated = notificationRepository.save(notification);
        return mapToDto(updated);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        if (userId != null) {
            notificationRepository.markAllAsReadForUser(userId);
        }
    }

    private NotificationResponseDto mapToDto(Notification entity) {
        return new NotificationResponseDto(
                entity.getIdNotificacion(),
                entity.getIdUsuario(),
                entity.getTitulo(),
                entity.getMensaje(),
                entity.getTipo(),
                entity.getLeido(),
                entity.getUrlDestino(),
                entity.getMetadata(),
                entity.getCreadoEn()
        );
    }
}
