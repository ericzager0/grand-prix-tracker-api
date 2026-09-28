package com.uade.grandprixtracker.notification.dispatcher;

import com.uade.grandprixtracker.notification.channel.NotificationChannel;
import com.uade.grandprixtracker.notification.strategy.NotificationContentStrategy;
import com.uade.grandprixtracker.notification.strategy.NotificationPayload;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orquestador central del despacho de notificaciones.
 * Conecta el evento recibido por el Observer con la Estrategia de Contenido adecuada
 * y luego despacha el resultado a todos los Canales registrados.
 */
@Service
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    @SuppressWarnings("rawtypes")
    private final List<NotificationContentStrategy> strategies;
    private final List<NotificationChannel> channels;

    @SuppressWarnings("rawtypes")
    public NotificationDispatcher(
            List<NotificationContentStrategy> strategies,
            List<NotificationChannel> channels) {
        this.strategies = strategies;
        this.channels = channels;
    }

    @SuppressWarnings("unchecked")
    public void dispatch(Object event) {
        NotificationContentStrategy<Object> strategy = strategies.stream()
                .filter(s -> s.supports(event))
                .findFirst()
                .orElse(null);

        if (strategy == null) {
            log.warn("No se encontró ninguna estrategia para procesar el evento {}", event.getClass().getSimpleName());
            return;
        }

        NotificationPayload payload = strategy.createPayload(event);
        if (payload == null) {
            log.warn("La estrategia {} retornó un payload nulo para el evento {}", strategy.getClass().getSimpleName(), event);
            return;
        }

        if (payload.isPubSub()) {
            log.info("[DISPATCHER - PUB/SUB] Distribución global (Broadcast para todos los usuarios). Evento: {}, Título: '{}'",
                    event.getClass().getSimpleName(), payload.title());
        } else {
            log.info("[DISPATCHER - POINT_TO_POINT] Distribución punto a punto para usuario={}. Evento: {}, Título: '{}'",
                    payload.userId(), event.getClass().getSimpleName(), payload.title());
        }

        for (NotificationChannel channel : channels) {
            try {
                channel.send(payload);
            } catch (Exception e) {
                log.error("Error al despachar notificación a través del canal {}: {}", channel.getChannelName(), e.getMessage(), e);
            }
        }
    }
}
