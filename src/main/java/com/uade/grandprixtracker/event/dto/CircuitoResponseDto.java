package com.uade.grandprixtracker.event.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CircuitoResponseDto(
        UUID idCircuito,
        String nombre,
        BigDecimal longitudKm,
        Integer curvas,
        Integer vueltas,
        String mapaSvgUrl,
        CiudadResponseDto ciudad
) {}

