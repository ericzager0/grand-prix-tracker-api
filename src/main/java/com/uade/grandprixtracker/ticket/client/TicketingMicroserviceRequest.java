package com.uade.grandprixtracker.ticket.client;

import java.util.UUID;

public record TicketingMicroserviceRequest(
        UUID idEntrada,
        int cantidad
) {}

