package com.uade.grandprixtracker.notification.event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Evento de dominio emitido cuando una reserva de entradas o evento es confirmada.
 */
public record BookingConfirmedEvent(
        UUID bookingId,
        UUID userId,
        String grandPrixName,
        Integer seatsCount,
        BigDecimal totalAmount
) {}
