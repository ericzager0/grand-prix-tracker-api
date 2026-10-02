package com.uade.grandprixtracker.booking.dto;

import com.uade.grandprixtracker.payment.dto.PagoRequestDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CheckoutRequestDto(
        @NotNull UUID idEvento,
        List<@Valid @NotNull EntradaItem> entradas,
        List<@Valid @NotNull HabitacionItem> habitaciones,
        List<@Valid @NotNull VueloItem> vuelos,
        @Valid @NotNull PagoRequestDto pago,
        Boolean incluyeTransporte
) {

    public CheckoutRequestDto {
        entradas = entradas == null ? List.of() : entradas;
        habitaciones = habitaciones == null ? List.of() : habitaciones;
        vuelos = vuelos == null ? List.of() : vuelos;
        incluyeTransporte = Boolean.TRUE.equals(incluyeTransporte);
    }

    public CheckoutRequestDto(UUID idEvento, List<EntradaItem> entradas, List<HabitacionItem> habitaciones, List<VueloItem> vuelos, PagoRequestDto pago) {
        this(idEvento, entradas, habitaciones, vuelos, pago, false);
    }

    public record EntradaItem(
            @NotNull UUID idEntrada,
            @NotNull @Positive Integer cantidad
    ) {}

    public record HabitacionItem(
            @NotNull UUID idHabitacion,
            @NotNull LocalDate fechaCheckIn,
            @NotNull LocalDate fechaCheckOut
    ) {}

    public record VueloItem(
            @NotNull UUID idVuelo,
            @NotNull @Positive Integer cantidadPasajeros
    ) {}
}
