package com.uade.grandprixtracker.booking.dto;

import com.uade.grandprixtracker.payment.dto.PagoRequestDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Topes por compra (regla de negocio): hasta {@value #MAX_ITEMS} ítems de cada tipo, hasta
 * {@value #MAX_CANTIDAD} entradas por tribuna y pasajeros por vuelo. La estadía máxima
 * ({@value #MAX_NOCHES} noches) se valida en el checkout.
 */
public record CheckoutRequestDto(
        @NotNull UUID idEvento,
        @Size(max = MAX_ITEMS, message = "no puede tener más de " + MAX_ITEMS + " ítems")
        List<@Valid @NotNull EntradaItem> entradas,
        @Size(max = MAX_ITEMS, message = "no puede tener más de " + MAX_ITEMS + " ítems")
        List<@Valid @NotNull HabitacionItem> habitaciones,
        @Size(max = MAX_ITEMS, message = "no puede tener más de " + MAX_ITEMS + " ítems")
        List<@Valid @NotNull VueloItem> vuelos,
        @Valid @NotNull PagoRequestDto pago,
        Boolean incluyeTransporte
) {

    public static final int MAX_ITEMS = 10;
    public static final int MAX_CANTIDAD = 10;
    public static final int MAX_NOCHES = 30;

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
            @NotNull @Positive @Max(value = MAX_CANTIDAD, message = "no puede superar " + MAX_CANTIDAD) Integer cantidad
    ) {}

    public record HabitacionItem(
            @NotNull UUID idHabitacion,
            @NotNull LocalDate fechaCheckIn,
            @NotNull LocalDate fechaCheckOut
    ) {}

    public record VueloItem(
            @NotNull UUID idVuelo,
            @NotNull @Positive @Max(value = MAX_CANTIDAD, message = "no puede superar " + MAX_CANTIDAD) Integer cantidadPasajeros
    ) {}
}
