package com.uade.grandprixtracker.notification.dto;

import com.uade.grandprixtracker.notification.event.PurchaseStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record SimulatePurchaseStatusEventDto(
        UUID userId,
        String productName,
        PurchaseStatus previousStatus,
        PurchaseStatus newStatus,
        BigDecimal totalPrice
) {}
