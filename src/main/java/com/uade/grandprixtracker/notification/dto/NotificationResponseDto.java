package com.uade.grandprixtracker.notification.dto;

import com.uade.grandprixtracker.notification.model.NotificationType;
import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationResponseDto(
        UUID idNotificacion,
        UUID idUsuario,
        String titulo,
        String mensaje,
        NotificationType tipo,
        Boolean leido,
        String urlDestino,
        String metadata,
        OffsetDateTime creadoEn
) {}
