package com.uade.grandprixtracker.booking.service;

import com.uade.grandprixtracker.booking.dto.ReservaResponseDto;
import com.uade.grandprixtracker.booking.model.Reserva;
import com.uade.grandprixtracker.event.model.Circuito;
import com.uade.grandprixtracker.event.model.Ciudad;
import com.uade.grandprixtracker.event.model.EventoF1;
import com.uade.grandprixtracker.payment.model.MetodoPago;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Arma el {@link ReservaResponseDto} que comparten el checkout y las consultas de reservas.
 * Espera la reserva con sus detalles y relaciones ya cargados (dentro de la transacción).
 */
final class ReservaMapper {

    private ReservaMapper() {
    }

    static ReservaResponseDto toDto(Reserva reserva) {
        MetodoPago metodoPago = reserva.getMetodoPago();
        return new ReservaResponseDto(
                reserva.getIdReserva(),
                reserva.getCodigoConfirmacion(),
                reserva.getEstado(),
                reserva.getFechaCompra(),
                reserva.getTotalUsd(),
                reserva.isIncluyeEntrada(),
                reserva.isIncluyeHotel(),
                reserva.isIncluyeVuelo(),
                metodoPago == null ? null : metodoPago.getIdMetodo(),
                metodoPago == null ? null : new ReservaResponseDto.MetodoPagoResumen(
                        metodoPago.getIdMetodo(),
                        metodoPago.getTipo(),
                        metodoPago.getUltimos4Digitos()),
                toEvento(reserva.getEvento()),
                reserva.getEntradas().stream()
                        .map(d -> new ReservaResponseDto.EntradaLinea(
                                d.getEntrada().getIdEntrada(),
                                d.getEntrada().getNombreTribuna(),
                                d.getEntrada().getTipo(),
                                d.getCantidad(),
                                precioUnitario(d.getSubtotalUsd(), d.getCantidad()),
                                d.getSubtotalUsd()))
                        .toList(),
                reserva.getHabitaciones().stream()
                        .map(d -> new ReservaResponseDto.HabitacionLinea(
                                d.getHabitacion().getIdHabitacion(),
                                d.getHabitacion().getHotel().getIdHotel(),
                                d.getHabitacion().getHotel().getNombre(),
                                d.getHabitacion().getTipo(),
                                d.getFechaCheckIn(),
                                d.getFechaCheckOut(),
                                d.getCantidadNoches(),
                                precioUnitario(d.getSubtotalUsd(), d.getCantidadNoches()),
                                d.getSubtotalUsd()))
                        .toList(),
                reserva.getVuelos().stream()
                        .map(d -> new ReservaResponseDto.VueloLinea(
                                d.getVuelo().getIdVuelo(),
                                d.getVuelo().getAerolinea(),
                                d.getVuelo().getOrigen().getNombre(),
                                d.getVuelo().getDestino().getNombre(),
                                d.getVuelo().getFechaSalida(),
                                d.getVuelo().getFechaLlegada(),
                                d.getCantidadPasajeros(),
                                precioUnitario(d.getSubtotalUsd(), d.getCantidadPasajeros()),
                                d.getSubtotalUsd()))
                        .toList()
        );
    }

    private static ReservaResponseDto.EventoResumen toEvento(EventoF1 evento) {
        if (evento == null) {
            return null;
        }
        Circuito circuito = evento.getCircuito();
        Ciudad ciudad = circuito.getCiudad();
        return new ReservaResponseDto.EventoResumen(
                evento.getIdEvento(),
                evento.getTemporada(),
                evento.getFechaInicio(),
                evento.getFechaFin(),
                new ReservaResponseDto.CircuitoResumen(
                        circuito.getNombre(),
                        new ReservaResponseDto.CiudadResumen(
                                ciudad.getNombre(),
                                new ReservaResponseDto.PaisResumen(
                                        ciudad.getPais().getNombre(),
                                        ciudad.getPais().getCodigoIso()))));
    }

    // Los detalles no guardan el precio unitario, solo el subtotal de la compra (precio × cantidad),
    // así que se recalcula desde ahí para no mostrar el precio actual del producto.
    private static BigDecimal precioUnitario(BigDecimal subtotal, int cantidad) {
        return subtotal.divide(BigDecimal.valueOf(cantidad), subtotal.scale(), RoundingMode.HALF_UP);
    }
}
