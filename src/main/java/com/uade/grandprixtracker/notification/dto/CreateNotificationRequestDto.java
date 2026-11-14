package com.uade.grandprixtracker.notification.dto;

import com.uade.grandprixtracker.notification.model.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Las validaciones aplican a lo que llega por POST /notifications. Ahí {@code idUsuario} se ignora:
 * el destinatario es siempre el usuario del token. Solo el backend (canal in-app) lo usa,
 * con {@code null} para las notificaciones globales.
 */
public record CreateNotificationRequestDto(
        UUID idUsuario,

        @NotBlank(message = "el título es obligatorio")
        @Size(max = 150, message = "no puede superar los 150 caracteres")
        String titulo,

        @NotBlank(message = "el mensaje es obligatorio")
        @Size(max = 1000, message = "no puede superar los 1000 caracteres")
        String mensaje,

        NotificationType tipo,

        // Solo rutas internas del front ("/algo"): ni URLs absolutas, ni "//host", ni "/\host".
        @Size(max = 255, message = "no puede superar los 255 caracteres")
        @Pattern(regexp = "^/(?![/\\\\]).*$", message = "debe ser una ruta interna que empiece con /")
        String urlDestino,

        @Size(max = 2000, message = "no puede superar los 2000 caracteres")
        String metadata
) {}
