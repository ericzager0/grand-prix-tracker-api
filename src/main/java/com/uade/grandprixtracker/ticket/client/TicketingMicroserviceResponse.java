package com.uade.grandprixtracker.ticket.client;

import java.math.BigDecimal;
import java.util.UUID;

public record TicketingMicroserviceResponse(
        String codigoConfirmacion,
        UUID idEntrada,
        UUID idEvento,
        String codigoEvento,
        String carrera,
        String nombreTribuna,
        String tipo,
        Integer cantidad,
        BigDecimal precioUnitarioUsd,
        BigDecimal precioTotalUsd,
        String estado,
        String mensaje,
        String fechaReserva
) {}

