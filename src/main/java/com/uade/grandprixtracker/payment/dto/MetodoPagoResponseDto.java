package com.uade.grandprixtracker.payment.dto;

import java.util.UUID;

public record MetodoPagoResponseDto(
        UUID idMetodoPago,
        String tipo,
        String ultimos4Digitos,
        String fechaExpiracion,
        boolean vencida
) {}
