package com.uade.grandprixtracker.booking.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import com.uade.grandprixtracker.booking.dto.ReservaResponseDto;
import com.uade.grandprixtracker.booking.model.Reserva;
import com.uade.grandprixtracker.booking.model.ReservaDetalleEntrada;
import com.uade.grandprixtracker.booking.model.ReservaDetalleHotel;
import com.uade.grandprixtracker.booking.model.ReservaDetalleVuelo;
import com.uade.grandprixtracker.booking.repository.ReservaRepository;
import com.uade.grandprixtracker.event.model.Circuito;
import com.uade.grandprixtracker.event.model.Ciudad;
import com.uade.grandprixtracker.event.model.EventoF1;
import com.uade.grandprixtracker.event.model.Pais;
import com.uade.grandprixtracker.flight.model.Vuelo;
import com.uade.grandprixtracker.hotel.model.HabitacionHotel;
import com.uade.grandprixtracker.hotel.model.Hotel;
import com.uade.grandprixtracker.payment.model.MetodoPago;
import com.uade.grandprixtracker.shared.exception.ResourceNotFoundException;
import com.uade.grandprixtracker.ticket.model.EntradaGrada;
import com.uade.grandprixtracker.user.model.Cliente;
import com.uade.grandprixtracker.user.repository.ClienteRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock private ReservaRepository reservaRepository;
    @Mock private ClienteRepository clienteRepository;

    private ReservaService reservaService;

    private UUID idCliente;
    private Cliente cliente;
    private Ciudad saoPaulo;
    private EventoF1 evento;
    private MetodoPago metodoPago;

    @BeforeEach
    void setUp() {
        reservaService = new ReservaService(reservaRepository, clienteRepository);

        idCliente = UUID.randomUUID();
        cliente = new Cliente(idCliente, "Ayrton", "Senna", "ayrton@example.com", null);
        saoPaulo = new Ciudad(UUID.randomUUID(), "Sao Paulo", new Pais(UUID.randomUUID(), "Brasil", "BR", "latin-america", null), null);
        Circuito circuito = new Circuito(UUID.randomUUID(), "Interlagos", null, null, null, null, saoPaulo, null);
        evento = new EventoF1(UUID.randomUUID(), 2026, LocalDate.of(2026, 11, 6), LocalDate.of(2026, 11, 8), "Proximo", circuito, null);
        metodoPago = new MetodoPago(UUID.randomUUID(), cliente, "Credito", "4242", "tok_secreto", "12/28", null);
    }

    private Reserva reserva(EventoF1 evento, String codigo, String fechaCompra) {
        return new Reserva(cliente, evento, metodoPago, codigo, Reserva.ESTADO_PAGADA, OffsetDateTime.parse(fechaCompra));
    }

    @Test
    void listarPorCliente_SinReservas_DevuelveListaVaciaSinCargarDetalles() {
        when(clienteRepository.existsById(idCliente)).thenReturn(true);
        when(reservaRepository.findByClienteConEvento(idCliente)).thenReturn(List.of());

        List<ReservaResponseDto> reservas = reservaService.listarPorCliente(idCliente);

        assertTrue(reservas.isEmpty());
        verify(reservaRepository, never()).fetchEntradas(anyList());
        verify(reservaRepository, never()).fetchHabitaciones(anyList());
        verify(reservaRepository, never()).fetchVuelos(anyList());
    }

    @Test
    void listarPorCliente_ConReservas_RespetaElOrdenPorFechaYCargaLosDetallesEnBloque() {
        Reserva nueva = reserva(evento, "GP-NUEVA234", "2026-10-02T12:00:00Z");
        Reserva vieja = reserva(evento, "GP-VIEJA234", "2026-09-01T12:00:00Z");
        List<Reserva> ordenadas = List.of(nueva, vieja);
        when(clienteRepository.existsById(idCliente)).thenReturn(true);
        when(reservaRepository.findByClienteConEvento(idCliente)).thenReturn(ordenadas);

        List<ReservaResponseDto> reservas = reservaService.listarPorCliente(idCliente);

        assertEquals(List.of("GP-NUEVA234", "GP-VIEJA234"),
                reservas.stream().map(ReservaResponseDto::codigoConfirmacion).toList());
        assertTrue(reservas.get(0).fechaCompra().isAfter(reservas.get(1).fechaCompra()));
        // Una query por colección para todas las reservas juntas, no una por reserva
        verify(reservaRepository, times(1)).fetchEntradas(ordenadas);
        verify(reservaRepository, times(1)).fetchHabitaciones(ordenadas);
        verify(reservaRepository, times(1)).fetchVuelos(ordenadas);
    }

    @Test
    void listarPorCliente_ClienteInexistente_LanzaNotFoundEnEspanolSinId() {
        when(clienteRepository.existsById(idCliente)).thenReturn(false);

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> reservaService.listarPorCliente(idCliente));

        assertEquals("Cliente no encontrado", ex.getMessage());
        verifyNoInteractions(reservaRepository);
    }

    @Test
    void obtener_ReservaDelCliente_MapeaLineasConPreciosDeLaCompraYMedioDePago() {
        Reserva reserva = reserva(evento, "GP-ABCD2345", "2026-10-01T12:00:00Z");

        // Precios actuales distintos de los de la compra: la respuesta tiene que usar los de la compra.
        EntradaGrada entrada = new EntradaGrada(UUID.randomUUID(), evento.getIdEvento(), "Tribuna A", new BigDecimal("999.00"), 10, "General");
        reserva.agregarEntrada(new ReservaDetalleEntrada(entrada, 2, new BigDecimal("700.00")));

        Hotel hotel = new Hotel(UUID.randomUUID(), saoPaulo.getIdCiudad(), "Hotel Paulista");
        HabitacionHotel habitacion = new HabitacionHotel(UUID.randomUUID(), hotel, "Doble", new BigDecimal("999.00"), 5);
        reserva.agregarHabitacion(new ReservaDetalleHotel(habitacion, LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 9), 4, new BigDecimal("800.00")));

        Ciudad buenosAires = new Ciudad(UUID.randomUUID(), "Buenos Aires", null, null);
        Vuelo vuelo = new Vuelo(UUID.randomUUID(), "LATAM", buenosAires, saoPaulo,
                OffsetDateTime.parse("2026-11-05T10:00:00Z"), OffsetDateTime.parse("2026-11-05T13:00:00Z"), new BigDecimal("999.00"), 50);
        reserva.agregarVuelo(new ReservaDetalleVuelo(vuelo, 2, new BigDecimal("1000.00")));

        UUID idReserva = UUID.randomUUID();
        when(clienteRepository.existsById(idCliente)).thenReturn(true);
        when(reservaRepository.findByIdAndClienteConEvento(idReserva, idCliente)).thenReturn(Optional.of(reserva));

        ReservaResponseDto dto = reservaService.obtener(idCliente, idReserva);

        assertEquals(new BigDecimal("2500.00"), dto.totalUsd());
        assertEquals(new BigDecimal("350.00"), dto.entradas().getFirst().precioUnitarioUsd());
        assertEquals(new BigDecimal("700.00"), dto.entradas().getFirst().subtotalUsd());
        assertEquals(new BigDecimal("200.00"), dto.habitaciones().getFirst().precioPorNocheUsd());
        assertEquals(hotel.getIdHotel(), dto.habitaciones().getFirst().idHotel());
        assertEquals("Hotel Paulista", dto.habitaciones().getFirst().hotel());
        assertEquals(4, dto.habitaciones().getFirst().cantidadNoches());
        assertEquals(new BigDecimal("500.00"), dto.vuelos().getFirst().precioUnitarioUsd());
        assertEquals("Buenos Aires", dto.vuelos().getFirst().origen());
        assertEquals(new ReservaResponseDto.MetodoPagoResumen(metodoPago.getIdMetodo(), "Credito", "4242"), dto.metodoPago());
        assertEquals(metodoPago.getIdMetodo(), dto.idMetodoPago());
        assertEquals("Interlagos", dto.evento().circuito().nombre());
        assertEquals("Brasil", dto.evento().circuito().ciudad().pais().nombre());
        verify(reservaRepository).fetchEntradas(List.of(reserva));
    }

    @Test
    void obtener_ReservaDeOtroClienteOInexistente_LanzaNotFoundEnEspanolSinId() {
        UUID idReserva = UUID.randomUUID();
        when(clienteRepository.existsById(idCliente)).thenReturn(true);
        // La query filtra por id y cliente: una reserva ajena no se encuentra.
        when(reservaRepository.findByIdAndClienteConEvento(idReserva, idCliente)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> reservaService.obtener(idCliente, idReserva));

        assertEquals("Reserva no encontrada", ex.getMessage());
        verify(reservaRepository, never()).fetchEntradas(anyList());
    }

    @Test
    void obtener_ClienteInexistente_LanzaNotFound() {
        when(clienteRepository.existsById(idCliente)).thenReturn(false);

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> reservaService.obtener(idCliente, UUID.randomUUID()));

        assertEquals("Cliente no encontrado", ex.getMessage());
        verifyNoInteractions(reservaRepository);
    }

    @Test
    void obtener_ReservaSinEventoNiMedioDePago_DevuelveNullEnEsosCampos() {
        Reserva reserva = new Reserva(cliente, null, null, "GP-SINEVT23", Reserva.ESTADO_PAGADA, OffsetDateTime.parse("2026-09-01T12:00:00Z"));
        UUID idReserva = UUID.randomUUID();
        when(clienteRepository.existsById(idCliente)).thenReturn(true);
        when(reservaRepository.findByIdAndClienteConEvento(idReserva, idCliente)).thenReturn(Optional.of(reserva));

        ReservaResponseDto dto = reservaService.obtener(idCliente, idReserva);

        assertNull(dto.evento());
        assertNull(dto.metodoPago());
        assertNull(dto.idMetodoPago());
        assertEquals("GP-SINEVT23", dto.codigoConfirmacion());
    }
}
