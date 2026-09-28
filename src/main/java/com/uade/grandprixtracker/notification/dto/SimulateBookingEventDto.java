package com.uade.grandprixtracker.notification.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SimulateBookingEventDto(
        UUID userId,
        String grandPrixName,
        Integer seatsCount,
        BigDecimal totalAmount
) {}
