package com.uade.grandprixtracker.booking.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CheckoutResponseDto(
        UUID idReserva,
        String codigoConfirmacion,
        String estado,
        OffsetDateTime fechaCompra,
        BigDecimal totalUsd,
        boolean incluyeEntrada,
        boolean incluyeHotel,
        boolean incluyeVuelo,
        UUID idMetodoPago,
        List<EntradaLinea> entradas,
        List<HabitacionLinea> habitaciones,
        List<VueloLinea> vuelos
) {

    public record EntradaLinea(
            UUID idEntrada,
            String nombreTribuna,
            String tipo,
            int cantidad,
            BigDecimal precioUnitarioUsd,
            BigDecimal subtotalUsd
    ) {}

    public record HabitacionLinea(
            UUID idHabitacion,
            String hotel,
            String tipo,
            LocalDate fechaCheckIn,
            LocalDate fechaCheckOut,
            int cantidadNoches,
            BigDecimal precioPorNocheUsd,
            BigDecimal subtotalUsd
    ) {}

    public record VueloLinea(
            UUID idVuelo,
            String aerolinea,
            String origen,
            String destino,
            OffsetDateTime fechaSalida,
            OffsetDateTime fechaLlegada,
            int cantidadPasajeros,
            BigDecimal precioUnitarioUsd,
            BigDecimal subtotalUsd
    ) {}
}
