package com.uade.grandprixtracker.notification.event;

import java.util.UUID;

/**
 * Evento de dominio emitido cuando se publica una nueva oferta o descuento.
 */
public record OfferPublishedEvent(
        UUID offerId,
        String title,
        String description,
        Integer discountPercentage,
        UUID targetUserId,
        String destinationUrl
) {}
