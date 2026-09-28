package com.uade.grandprixtracker.booking.service;

import com.uade.grandprixtracker.booking.dto.CheckoutRequestDto;
import com.uade.grandprixtracker.booking.dto.CheckoutRequestDto.EntradaItem;
import com.uade.grandprixtracker.booking.dto.CheckoutRequestDto.HabitacionItem;
import com.uade.grandprixtracker.booking.dto.CheckoutRequestDto.VueloItem;
import com.uade.grandprixtracker.booking.dto.CheckoutResponseDto;
import com.uade.grandprixtracker.booking.model.Reserva;
import com.uade.grandprixtracker.booking.model.ReservaDetalleEntrada;
import com.uade.grandprixtracker.booking.model.ReservaDetalleHotel;
import com.uade.grandprixtracker.booking.model.ReservaDetalleVuelo;
import com.uade.grandprixtracker.booking.repository.ReservaRepository;
import com.uade.grandprixtracker.event.model.EventoF1;
import com.uade.grandprixtracker.event.repository.EventoF1Repository;
import com.uade.grandprixtracker.flight.model.Vuelo;
import com.uade.grandprixtracker.flight.service.FlightService;
import com.uade.grandprixtracker.hotel.model.HabitacionHotel;
import com.uade.grandprixtracker.hotel.service.HotelService;
import com.uade.grandprixtracker.payment.model.MetodoPago;
import com.uade.grandprixtracker.payment.service.PaymentService;
import com.uade.grandprixtracker.shared.exception.ResourceNotFoundException;
import com.uade.grandprixtracker.ticket.model.EntradaGrada;
import com.uade.grandprixtracker.ticket.service.TicketService;
import com.uade.grandprixtracker.user.model.Cliente;
import com.uade.grandprixtracker.user.repository.ClienteRepository;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Punto único de entrada del checkout: coordina entradas, hotel, vuelos y pago en una sola transacción.
 */
@Service
public class CheckoutFacade {

    private static final String ESTADO_EVENTO_FINALIZADO = "Finalizado";
    private static final String ALFABETO_CODIGO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LARGO_CODIGO = 8;

    private final ClienteRepository clienteRepository;
    private final EventoF1Repository eventoF1Repository;
    private final ReservaRepository reservaRepository;
    private final TicketService ticketService;
    private final HotelService hotelService;
    private final FlightService flightService;
    private final PaymentService paymentService;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public CheckoutFacade(ClienteRepository clienteRepository,
                          EventoF1Repository eventoF1Repository,
                          ReservaRepository reservaRepository,
                          TicketService ticketService,
                          HotelService hotelService,
                          FlightService flightService,
                          PaymentService paymentService,
                          Clock clock) {
        this.clienteRepository = clienteRepository;
        this.eventoF1Repository = eventoF1Repository;
        this.reservaRepository = reservaRepository;
        this.ticketService = ticketService;
        this.hotelService = hotelService;
        this.flightService = flightService;
        this.paymentService = paymentService;
        this.clock = clock;
    }

    @Transactional
    public CheckoutResponseDto checkout(UUID idCliente, CheckoutRequestDto request) {
        if (request.entradas().isEmpty() && request.habitaciones().isEmpty() && request.vuelos().isEmpty()) {
            throw new IllegalArgumentException("El paquete debe incluir al menos una entrada, habitación o vuelo");
        }

        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "id", idCliente));

        EventoF1 evento = eventoF1Repository.findById(request.idEvento())
                .orElseThrow(() -> new ResourceNotFoundException("Evento", "id", request.idEvento()));

        LocalDate hoy = LocalDate.now(clock);

        if (ESTADO_EVENTO_FINALIZADO.equals(evento.getEstado()) || evento.getFechaFin().isBefore(hoy)) {
            throw new IllegalArgumentException("El evento ya finalizó, no se pueden comprar paquetes");
        }

        UUID idCiudadEvento = evento.getCircuito().getCiudad().getIdCiudad();

        for (HabitacionItem item : request.habitaciones()) {
            if (!item.fechaCheckOut().isAfter(item.fechaCheckIn())) {
                throw new IllegalArgumentException("La fecha de check-out debe ser posterior a la de check-in");
            }
            if (item.fechaCheckIn().isBefore(hoy)) {
                throw new IllegalArgumentException("La fecha de check-in no puede ser anterior a hoy");
            }
        }

        MetodoPago metodoPago = paymentService.cobrar(cliente, request.pago());
        Reserva reserva = new Reserva(cliente, metodoPago, generarCodigoConfirmacion(), Reserva.ESTADO_PAGADA, OffsetDateTime.now(clock));

        // Se descuenta stock en orden de id para que dos checkouts concurrentes no se bloqueen mutuamente.
        request.entradas().stream()
                .sorted(Comparator.comparing(EntradaItem::idEntrada))
                .forEach(item -> {
                    EntradaGrada entrada = ticketService.reservar(evento.getIdEvento(), item.idEntrada(), item.cantidad());
                    BigDecimal subtotal = entrada.getPrecioUsd().multiply(BigDecimal.valueOf(item.cantidad()));
                    reserva.agregarEntrada(new ReservaDetalleEntrada(entrada, item.cantidad(), subtotal));
                });

        request.habitaciones().stream()
                .sorted(Comparator.comparing(HabitacionItem::idHabitacion))
                .forEach(item -> {
                    HabitacionHotel habitacion = hotelService.reservar(idCiudadEvento, item.idHabitacion());
                    int noches = (int) ChronoUnit.DAYS.between(item.fechaCheckIn(), item.fechaCheckOut());
                    BigDecimal subtotal = habitacion.getPrecioPorNocheUsd().multiply(BigDecimal.valueOf(noches));
                    reserva.agregarHabitacion(new ReservaDetalleHotel(habitacion, item.fechaCheckIn(), item.fechaCheckOut(), noches, subtotal));
                });

        request.vuelos().stream()
                .sorted(Comparator.comparing(VueloItem::idVuelo))
                .forEach(item -> {
                    Vuelo vuelo = flightService.reservar(idCiudadEvento, item.idVuelo(), item.cantidadPasajeros());
                    BigDecimal subtotal = vuelo.getPrecioUsd().multiply(BigDecimal.valueOf(item.cantidadPasajeros()));
                    reserva.agregarVuelo(new ReservaDetalleVuelo(vuelo, item.cantidadPasajeros(), subtotal));
                });

        return toDto(reservaRepository.save(reserva));
    }

    private String generarCodigoConfirmacion() {
        StringBuilder codigo = new StringBuilder("GP-");
        for (int i = 0; i < LARGO_CODIGO; i++) {
            codigo.append(ALFABETO_CODIGO.charAt(random.nextInt(ALFABETO_CODIGO.length())));
        }
        return codigo.toString();
    }

    private CheckoutResponseDto toDto(Reserva reserva) {
        return new CheckoutResponseDto(
                reserva.getIdReserva(),
                reserva.getCodigoConfirmacion(),
                reserva.getEstado(),
                reserva.getFechaCompra(),
                reserva.getTotalUsd(),
                reserva.isIncluyeEntrada(),
                reserva.isIncluyeHotel(),
                reserva.isIncluyeVuelo(),
                reserva.getMetodoPago().getIdMetodo(),
                reserva.getEntradas().stream()
                        .map(d -> new CheckoutResponseDto.EntradaLinea(
                                d.getEntrada().getIdEntrada(),
                                d.getEntrada().getNombreTribuna(),
                                d.getEntrada().getTipo(),
                                d.getCantidad(),
                                d.getEntrada().getPrecioUsd(),
                                d.getSubtotalUsd()))
                        .toList(),
                reserva.getHabitaciones().stream()
                        .map(d -> new CheckoutResponseDto.HabitacionLinea(
                                d.getHabitacion().getIdHabitacion(),
                                d.getHabitacion().getHotel().getNombre(),
                                d.getHabitacion().getTipo(),
                                d.getFechaCheckIn(),
                                d.getFechaCheckOut(),
                                d.getCantidadNoches(),
                                d.getHabitacion().getPrecioPorNocheUsd(),
                                d.getSubtotalUsd()))
                        .toList(),
                reserva.getVuelos().stream()
                        .map(d -> new CheckoutResponseDto.VueloLinea(
                                d.getVuelo().getIdVuelo(),
                                d.getVuelo().getAerolinea(),
                                d.getVuelo().getOrigen().getNombre(),
                                d.getVuelo().getDestino().getNombre(),
                                d.getVuelo().getFechaSalida(),
                                d.getVuelo().getFechaLlegada(),
                                d.getCantidadPasajeros(),
                                d.getVuelo().getPrecioUsd(),
                                d.getSubtotalUsd()))
                        .toList()
        );
    }
}
