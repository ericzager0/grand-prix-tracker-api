package com.uade.grandprixtracker.hotel.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record HotelResponseDto(
        UUID idHotel,
        String nombre,
        Integer estrellas,
        BigDecimal distanciaCircuitoKm,
        Boolean ofreceTraslado,
        String imagenPrincipalUrl,
        List<HabitacionResponseDto> habitaciones
) {

    public record HabitacionResponseDto(
            UUID idHabitacion,
            String tipo,
            BigDecimal precioPorNocheUsd,
            Integer stockDisponible
    ) {}
}
