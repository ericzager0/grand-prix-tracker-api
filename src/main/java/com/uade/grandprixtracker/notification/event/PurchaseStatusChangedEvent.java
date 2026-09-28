package com.uade.grandprixtracker.notification.event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Evento de dominio emitido cuando una orden de compra cambia de estado (ej: PENDING -> APPROVED).
 */
public record PurchaseStatusChangedEvent(
        UUID purchaseId,
        UUID userId,
        String productName,
        PurchaseStatus previousStatus,
        PurchaseStatus newStatus,
        BigDecimal totalPrice
) {}
