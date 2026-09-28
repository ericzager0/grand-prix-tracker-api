package com.uade.grandprixtracker.notification.dto;

import java.util.UUID;

public record SimulateOfferEventDto(
        String title,
        String description,
        Integer discountPercentage,
        UUID targetUserId,
        String destinationUrl
) {}
