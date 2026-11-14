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

    /**
     * Uso interno (canal in-app). Con idUsuario null la notificación es global.
     */
    @Transactional
    public NotificationResponseDto createNotification(CreateNotificationRequestDto dto) {
        return guardar(dto.idUsuario(), dto);
    }

    /**
     * Alta pedida por un cliente: el destinatario es siempre el usuario autenticado,
     * sin importar el idUsuario que venga en el body.
     */
    @Transactional
    public NotificationResponseDto createForUser(UUID idUsuario, CreateNotificationRequestDto dto) {
        if (idUsuario == null) {
            throw new IllegalArgumentException("El usuario es obligatorio");
        }
        return guardar(idUsuario, dto);
    }

    private NotificationResponseDto guardar(UUID idUsuario, CreateNotificationRequestDto dto) {
        Notification notification = new Notification(
                idUsuario,
                dto.titulo(),
                dto.mensaje(),
                dto.tipo() != null ? dto.tipo() : NotificationType.SYSTEM,
                dto.urlDestino(),
                dto.metadata()
        );
        return mapToDto(notificationRepository.save(notification));
    }

    /**
     * Solo el dueño puede marcar su notificación. Una de otro usuario se responde igual que una
     * inexistente, para no revelar que existe. Las globales (idUsuario null) son una única fila
     * compartida por todos: no se modifican, para que un usuario no las marque como leídas para el resto.
     */
    @Transactional
    public NotificationResponseDto markAsRead(UUID idUsuario, UUID idNotificacion) {
        Notification notification = notificationRepository.findById(idNotificacion)
                .filter(n -> n.getIdUsuario() == null || n.getIdUsuario().equals(idUsuario))
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada"));
        if (notification.getIdUsuario() == null) {
            return mapToDto(notification);
        }
        notification.setLeido(true);
        return mapToDto(notificationRepository.save(notification));
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
