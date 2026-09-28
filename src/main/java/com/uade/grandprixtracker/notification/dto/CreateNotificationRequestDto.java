package com.uade.grandprixtracker.notification.dto;

import com.uade.grandprixtracker.notification.model.NotificationType;
import java.util.UUID;

public record CreateNotificationRequestDto(
        UUID idUsuario,
        String titulo,
        String mensaje,
        NotificationType tipo,
        String urlDestino,
        String metadata
) {}
