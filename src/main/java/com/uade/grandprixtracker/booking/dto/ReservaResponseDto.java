package com.uade.grandprixtracker.booking.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Forma única de una reserva: la devuelven POST /bookings, GET /bookings y GET /bookings/{idReserva}.
 * Los precios unitarios y subtotales son los de la compra, no los precios actuales.
 */
public record ReservaResponseDto(
        UUID idReserva,
        String codigoConfirmacion,
        String estado,
        OffsetDateTime fechaCompra,
        BigDecimal totalUsd,
        boolean incluyeEntrada,
        boolean incluyeHotel,
        boolean incluyeVuelo,
        boolean incluyeTransporte,
        UUID idMetodoPago,
        MetodoPagoResumen metodoPago,
        EventoResumen evento,
        List<EntradaLinea> entradas,
        List<HabitacionLinea> habitaciones,
        List<VueloLinea> vuelos
) {

    public ReservaResponseDto(
            UUID idReserva,
            String codigoConfirmacion,
            String estado,
            OffsetDateTime fechaCompra,
            BigDecimal totalUsd,
            boolean incluyeEntrada,
            boolean incluyeHotel,
            boolean incluyeVuelo,
            UUID idMetodoPago,
            MetodoPagoResumen metodoPago,
            EventoResumen evento,
            List<EntradaLinea> entradas,
            List<HabitacionLinea> habitaciones,
            List<VueloLinea> vuelos
    ) {
        this(idReserva, codigoConfirmacion, estado, fechaCompra, totalUsd,
                incluyeEntrada, incluyeHotel, incluyeVuelo, false,
                idMetodoPago, metodoPago, evento, entradas, habitaciones, vuelos);
    }

    public record MetodoPagoResumen(
            UUID idMetodoPago,
            String tipo,
            String ultimos4Digitos
    ) {}

    public record EventoResumen(
            UUID idEvento,
            Integer temporada,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            CircuitoResumen circuito
    ) {}

    public record CircuitoResumen(
            String nombre,
            CiudadResumen ciudad
    ) {}

    public record CiudadResumen(
            String nombre,
            PaisResumen pais
    ) {}

    public record PaisResumen(
            String nombre,
            String codigoIso
    ) {}

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
            UUID idHotel,
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
