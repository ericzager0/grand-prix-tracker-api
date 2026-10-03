package com.uade.grandprixtracker.booking.event;

import com.uade.grandprixtracker.booking.dto.ReservaResponseDto;

/**
 * Evento de dominio publicado por el checkout cuando una compra se confirma.
 * El checkout no sabe quién lo escucha: así la compra no depende de las notificaciones.
 */
public record CompraConfirmadaEvent(
        String emailCliente,
        String nombreCliente,
        ReservaResponseDto reserva
) {}
