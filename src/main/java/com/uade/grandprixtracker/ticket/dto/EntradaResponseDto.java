package com.uade.grandprixtracker.ticket.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record EntradaResponseDto(
        UUID idEntrada,
        String nombreTribuna,
        String tipo,
        BigDecimal precioUsd,
        Integer stockDisponible
) {}
