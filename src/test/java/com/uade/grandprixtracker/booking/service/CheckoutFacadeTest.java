package com.uade.grandprixtracker.booking.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.uade.grandprixtracker.booking.dto.CheckoutRequestDto;
import com.uade.grandprixtracker.booking.dto.CheckoutRequestDto.EntradaItem;
import com.uade.grandprixtracker.booking.dto.CheckoutRequestDto.HabitacionItem;
import com.uade.grandprixtracker.booking.dto.CheckoutRequestDto.VueloItem;
import com.uade.grandprixtracker.booking.dto.ReservaResponseDto;
import com.uade.grandprixtracker.booking.model.Reserva;
import com.uade.grandprixtracker.booking.repository.ReservaRepository;
import com.uade.grandprixtracker.event.model.Circuito;
import com.uade.grandprixtracker.event.model.Ciudad;
import com.uade.grandprixtracker.event.model.EventoF1;
import com.uade.grandprixtracker.event.model.Pais;
import com.uade.grandprixtracker.event.repository.EventoF1Repository;
import com.uade.grandprixtracker.flight.model.Vuelo;
import com.uade.grandprixtracker.flight.service.FlightService;
import com.uade.grandprixtracker.hotel.model.HabitacionHotel;
import com.uade.grandprixtracker.hotel.model.Hotel;
import com.uade.grandprixtracker.hotel.service.HotelService;
import com.uade.grandprixtracker.payment.dto.PagoRequestDto;
import com.uade.grandprixtracker.payment.model.MetodoPago;
import com.uade.grandprixtracker.payment.service.PaymentService;
import com.uade.grandprixtracker.shared.exception.ResourceNotFoundException;
import com.uade.grandprixtracker.shared.exception.StockInsuficienteException;
import com.uade.grandprixtracker.ticket.model.EntradaGrada;
import com.uade.grandprixtracker.ticket.service.TicketService;
import com.uade.grandprixtracker.user.model.Cliente;
import com.uade.grandprixtracker.user.repository.ClienteRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CheckoutFacadeTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-01T12:00:00Z"), ZoneOffset.UTC);

    @Mock private ClienteRepository clienteRepository;
    @Mock private EventoF1Repository eventoF1Repository;
    @Mock private ReservaRepository reservaRepository;
    @Mock private TicketService ticketService;
    @Mock private HotelService hotelService;
    @Mock private FlightService flightService;
    @Mock private PaymentService paymentService;

    private CheckoutFacade checkoutFacade;

    private UUID idCliente;
    private UUID idEvento;
    private UUID idCiudad;
    private Ciudad ciudad;
    private Cliente cliente;
    private EventoF1 evento;
    private MetodoPago metodoPago;
    private PagoRequestDto pago;

    @BeforeEach
    void setUp() {
        checkoutFacade = new CheckoutFacade(clienteRepository, eventoF1Repository, reservaRepository,
                ticketService, hotelService, flightService, paymentService, CLOCK);

        idCliente = UUID.randomUUID();
        idEvento = UUID.randomUUID();
        idCiudad = UUID.randomUUID();

        cliente = new Cliente(idCliente, "Ayrton", "Senna", "ayrton@example.com", null);
        ciudad = new Ciudad(idCiudad, "São Paulo", new Pais(UUID.randomUUID(), "Brasil", "BR", "latin-america", null), null);
        Circuito circuito = new Circuito(UUID.randomUUID(), "Interlagos", null, null, null, null, ciudad, null);
        evento = new EventoF1(idEvento, 2026, LocalDate.of(2026, 11, 6), LocalDate.of(2026, 11, 8), "Proximo", circuito, null);
        metodoPago = new MetodoPago(UUID.randomUUID(), cliente, "Credito", "4242", "tok_test", "12/28", null);
        pago = new PagoRequestDto(metodoPago.getIdMetodo(), null, null, null, null);
    }

    private void stubClienteYEvento() {
        when(clienteRepository.findById(idCliente)).thenReturn(Optional.of(cliente));
        when(eventoF1Repository.findById(idEvento)).thenReturn(Optional.of(evento));
    }

    @Test
    void checkout_PaqueteCompleto_CalculaTotalesYGuardaReservaPagada() {
        stubClienteYEvento();
        when(paymentService.cobrar(cliente, pago)).thenReturn(metodoPago);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        EntradaGrada entrada = new EntradaGrada(UUID.randomUUID(), idEvento, "Tribuna A", new BigDecimal("350.00"), 10, "General");
        Hotel hotel = new Hotel(UUID.randomUUID(), idCiudad, "Hotel Paulista");
        HabitacionHotel habitacion = new HabitacionHotel(UUID.randomUUID(), hotel, "Doble", new BigDecimal("200.00"), 5);
        Ciudad buenosAires = new Ciudad(UUID.randomUUID(), "Buenos Aires", null, null);
        Vuelo vuelo = new Vuelo(UUID.randomUUID(), "LATAM", buenosAires, ciudad,
                OffsetDateTime.parse("2026-11-05T10:00:00Z"), OffsetDateTime.parse("2026-11-05T13:00:00Z"), new BigDecimal("500.00"), 50);

        when(ticketService.reservar(idEvento, entrada.getIdEntrada(), 2)).thenReturn(entrada);
        when(hotelService.reservar(idCiudad, habitacion.getIdHabitacion())).thenReturn(habitacion);
        when(flightService.reservar(idCiudad, vuelo.getIdVuelo(), 2)).thenReturn(vuelo);

        CheckoutRequestDto request = new CheckoutRequestDto(
                idEvento,
                List.of(new EntradaItem(entrada.getIdEntrada(), 2)),
                List.of(new HabitacionItem(habitacion.getIdHabitacion(), LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 9))),
                List.of(new VueloItem(vuelo.getIdVuelo(), 2)),
                pago);

        ReservaResponseDto response = checkoutFacade.checkout(idCliente, request);

        // 2 x 350 + 4 noches x 200 + 2 x 500
        assertEquals(new BigDecimal("2500.00"), response.totalUsd());
        assertEquals(Reserva.ESTADO_PAGADA, response.estado());
        assertTrue(response.incluyeEntrada());
        assertTrue(response.incluyeHotel());
        assertTrue(response.incluyeVuelo());
        assertTrue(response.codigoConfirmacion().matches("GP-[A-Z2-9]{8}"));
        assertEquals(metodoPago.getIdMetodo(), response.idMetodoPago());
        assertEquals(4, response.habitaciones().getFirst().cantidadNoches());
        assertEquals(new BigDecimal("800.00"), response.habitaciones().getFirst().subtotalUsd());
        assertEquals("Hotel Paulista", response.habitaciones().getFirst().hotel());
        assertEquals("Buenos Aires", response.vuelos().getFirst().origen());
        assertEquals("São Paulo", response.vuelos().getFirst().destino());

        // Precios unitarios de la compra
        assertEquals(new BigDecimal("350.00"), response.entradas().getFirst().precioUnitarioUsd());
        assertEquals(new BigDecimal("200.00"), response.habitaciones().getFirst().precioPorNocheUsd());
        assertEquals(new BigDecimal("500.00"), response.vuelos().getFirst().precioUnitarioUsd());

        // Campos nuevos del contrato
        assertEquals(hotel.getIdHotel(), response.habitaciones().getFirst().idHotel());
        assertEquals(metodoPago.getIdMetodo(), response.metodoPago().idMetodoPago());
        assertEquals("Credito", response.metodoPago().tipo());
        assertEquals("4242", response.metodoPago().ultimos4Digitos());
        assertEquals(idEvento, response.evento().idEvento());
        assertEquals(LocalDate.of(2026, 11, 6), response.evento().fechaInicio());
        assertEquals("Interlagos", response.evento().circuito().nombre());
        assertEquals("São Paulo", response.evento().circuito().ciudad().nombre());
        assertEquals("BR", response.evento().circuito().ciudad().pais().codigoIso());

        // El evento queda persistido en la reserva
        ArgumentCaptor<Reserva> guardada = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).save(guardada.capture());
        assertSame(evento, guardada.getValue().getEvento());
        assertFalse(response.incluyeTransporte());
        assertFalse(guardada.getValue().isIncluyeTransporte());
    }

    @Test
    void checkout_SoloEntradas_MarcaSoloIncluyeEntrada() {
        stubClienteYEvento();
        when(paymentService.cobrar(cliente, pago)).thenReturn(metodoPago);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        EntradaGrada entrada = new EntradaGrada(UUID.randomUUID(), idEvento, "VIP", new BigDecimal("1200.00"), 3, "VIP");
        when(ticketService.reservar(idEvento, entrada.getIdEntrada(), 1)).thenReturn(entrada);

        CheckoutRequestDto request = new CheckoutRequestDto(
                idEvento, List.of(new EntradaItem(entrada.getIdEntrada(), 1)), null, null, pago);

        ReservaResponseDto response = checkoutFacade.checkout(idCliente, request);

        assertEquals(new BigDecimal("1200.00"), response.totalUsd());
        assertTrue(response.incluyeEntrada());
        assertFalse(response.incluyeHotel());
        assertFalse(response.incluyeVuelo());
        verifyNoInteractions(hotelService, flightService);
    }

    @Test
    void checkout_PaqueteVacio_LanzaIllegalArgument() {
        CheckoutRequestDto request = new CheckoutRequestDto(idEvento, null, null, null, pago);

        assertThrows(IllegalArgumentException.class, () -> checkoutFacade.checkout(idCliente, request));
        verifyNoInteractions(clienteRepository, paymentService, reservaRepository);
    }

    @Test
    void checkout_ClienteInexistente_LanzaNotFound() {
        when(clienteRepository.findById(idCliente)).thenReturn(Optional.empty());
        CheckoutRequestDto request = new CheckoutRequestDto(
                idEvento, List.of(new EntradaItem(UUID.randomUUID(), 1)), null, null, pago);

        assertThrows(ResourceNotFoundException.class, () -> checkoutFacade.checkout(idCliente, request));
    }

    @Test
    void checkout_EventoFinalizado_LanzaIllegalArgumentSinCobrar() {
        evento.setEstado("Finalizado");
        stubClienteYEvento();
        CheckoutRequestDto request = new CheckoutRequestDto(
                idEvento, List.of(new EntradaItem(UUID.randomUUID(), 1)), null, null, pago);

        assertThrows(IllegalArgumentException.class, () -> checkoutFacade.checkout(idCliente, request));
        verifyNoInteractions(paymentService, ticketService, reservaRepository);
    }

    @Test
    void checkout_EventoPasadoAunqueFigureComoProximo_LanzaIllegalArgument() {
        evento.setFechaInicio(LocalDate.of(2026, 9, 25));
        evento.setFechaFin(LocalDate.of(2026, 9, 27));
        stubClienteYEvento();
        CheckoutRequestDto request = new CheckoutRequestDto(
                idEvento, List.of(new EntradaItem(UUID.randomUUID(), 1)), null, null, pago);

        assertThrows(IllegalArgumentException.class, () -> checkoutFacade.checkout(idCliente, request));
        verifyNoInteractions(paymentService, ticketService, reservaRepository);
    }

    @Test
    void checkout_CheckOutNoPosteriorACheckIn_LanzaIllegalArgument() {
        stubClienteYEvento();
        CheckoutRequestDto request = new CheckoutRequestDto(
                idEvento, null,
                List.of(new HabitacionItem(UUID.randomUUID(), LocalDate.of(2026, 11, 8), LocalDate.of(2026, 11, 8))),
                null, pago);

        assertThrows(IllegalArgumentException.class, () -> checkoutFacade.checkout(idCliente, request));
        verifyNoInteractions(paymentService, hotelService);
    }

    @Test
    void checkout_CheckInEnElPasado_LanzaIllegalArgument() {
        stubClienteYEvento();
        CheckoutRequestDto request = new CheckoutRequestDto(
                idEvento, null,
                List.of(new HabitacionItem(UUID.randomUUID(), LocalDate.of(2026, 9, 30), LocalDate.of(2026, 10, 2))),
                null, pago);

        assertThrows(IllegalArgumentException.class, () -> checkoutFacade.checkout(idCliente, request));
    }

    @Test
    void checkout_SinStock_PropagaExcepcionYNoGuardaReserva() {
        stubClienteYEvento();
        when(paymentService.cobrar(cliente, pago)).thenReturn(metodoPago);
        UUID idEntrada = UUID.randomUUID();
        when(ticketService.reservar(idEvento, idEntrada, 5)).thenThrow(new StockInsuficienteException("sin stock"));

        CheckoutRequestDto request = new CheckoutRequestDto(
                idEvento, List.of(new EntradaItem(idEntrada, 5)), null, null, pago);

        assertThrows(StockInsuficienteException.class, () -> checkoutFacade.checkout(idCliente, request));
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void checkout_ConTransporte_Suma30UsdAlTotalYMarcaIncluyeTransporte() {
        stubClienteYEvento();
        when(paymentService.cobrar(cliente, pago)).thenReturn(metodoPago);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        EntradaGrada entrada = new EntradaGrada(UUID.randomUUID(), idEvento, "Tribuna A", new BigDecimal("350.00"), 10, "General");
        when(ticketService.reservar(idEvento, entrada.getIdEntrada(), 1)).thenReturn(entrada);

        CheckoutRequestDto request = new CheckoutRequestDto(
                idEvento,
                List.of(new EntradaItem(entrada.getIdEntrada(), 1)),
                List.of(),
                List.of(),
                pago,
                true);

        ReservaResponseDto response = checkoutFacade.checkout(idCliente, request);

        // 350 + 30 USD transporte = 380.00
        assertEquals(new BigDecimal("380.00"), response.totalUsd());
        assertTrue(response.incluyeTransporte());

        ArgumentCaptor<Reserva> guardada = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).save(guardada.capture());
        assertTrue(guardada.getValue().isIncluyeTransporte());
        assertEquals(new BigDecimal("380.00"), guardada.getValue().getTotalUsd());
    }
}
