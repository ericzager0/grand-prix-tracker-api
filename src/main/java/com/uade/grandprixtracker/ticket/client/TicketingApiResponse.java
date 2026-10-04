package com.uade.grandprixtracker.ticket.client;

public record TicketingApiResponse<T>(
        boolean success,
        String message,
        T data
) {}
