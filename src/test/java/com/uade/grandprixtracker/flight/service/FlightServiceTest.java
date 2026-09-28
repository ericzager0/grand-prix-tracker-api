package com.uade.grandprixtracker.flight.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import com.uade.grandprixtracker.event.model.Circuito;
import com.uade.grandprixtracker.event.model.Ciudad;
import com.uade.grandprixtracker.event.model.EventoF1;
import com.uade.grandprixtracker.event.repository.EventoF1Repository;
import com.uade.grandprixtracker.flight.dto.VueloResponseDto;
import com.uade.grandprixtracker.flight.model.Vuelo;
import com.uade.grandprixtracker.flight.repository.VueloRepository;
import com.uade.grandprixtracker.shared.exception.ResourceNotFoundException;
import com.uade.grandprixtracker.shared.exception.StockInsuficienteException;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FlightServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-01T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private VueloRepository vueloRepository;

    @Mock
    private EventoF1Repository eventoF1Repository;

    private FlightService flightService;
    private Ciudad ciudadEvento;
    private Ciudad buenosAires;

    @BeforeEach
    void setUp() {
        flightService = new FlightService(vueloRepository, eventoF1Repository, CLOCK);
        ciudadEvento = new Ciudad(UUID.randomUUID(), "São Paulo", null, null);
        buenosAires = new Ciudad(UUID.randomUUID(), "Buenos Aires", null, null);
    }

    private Vuelo vuelo(Ciudad origen, Ciudad destino, String salida) {
        return new Vuelo(UUID.randomUUID(), "LATAM", origen, destino,
                OffsetDateTime.parse(salida), OffsetDateTime.parse(salida).plusHours(3), new BigDecimal("500.00"), 10);
    }

    private Vuelo vueloEnRepo(Ciudad origen, Ciudad destino, String salida) {
        Vuelo vuelo = vuelo(origen, destino, salida);
        when(vueloRepository.findByIdWithCiudades(vuelo.getIdVuelo())).thenReturn(Optional.of(vuelo));
        return vuelo;
    }

    @Test
    void listarPorEvento_MarcaSentidoIdaYVuelta() {
        UUID idEvento = UUID.randomUUID();
        Circuito circuito = new Circuito(UUID.randomUUID(), "Interlagos", null, null, null, null, ciudadEvento, null);
        EventoF1 evento = new EventoF1(idEvento, 2026, LocalDate.of(2026, 11, 6), LocalDate.of(2026, 11, 8), "Proximo", circuito, null);
        Vuelo ida = vuelo(buenosAires, ciudadEvento, "2026-11-05T12:00:00Z");
        Vuelo vuelta = vuelo(ciudadEvento, buenosAires, "2026-11-09T19:00:00Z");
        when(eventoF1Repository.findById(idEvento)).thenReturn(Optional.of(evento));
        when(vueloRepository.findDisponiblesPorCiudad(ciudadEvento.getIdCiudad(), OffsetDateTime.now(CLOCK)))
                .thenReturn(List.of(ida, vuelta));

        List<VueloResponseDto> vuelos = flightService.listarPorEvento(idEvento);

        assertEquals(2, vuelos.size());
        assertEquals(VueloResponseDto.Sentido.IDA, vuelos.get(0).sentido());
        assertEquals("Buenos Aires", vuelos.get(0).origen().nombre());
        assertEquals("São Paulo", vuelos.get(0).destino().nombre());
        assertEquals(VueloResponseDto.Sentido.VUELTA, vuelos.get(1).sentido());
    }

    @Test
    void listarPorEvento_EventoInexistente_LanzaNotFound() {
        UUID idEvento = UUID.randomUUID();
        when(eventoF1Repository.findById(idEvento)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> flightService.listarPorEvento(idEvento));
    }

    @Test
    void reservar_VueloDeIdaALaCiudadDelEvento_Descuenta() {
        Vuelo ida = vueloEnRepo(buenosAires, ciudadEvento, "2026-11-05T10:00:00Z");
        when(vueloRepository.descontarStock(ida.getIdVuelo(), 2)).thenReturn(1);

        assertSame(ida, flightService.reservar(ciudadEvento.getIdCiudad(), ida.getIdVuelo(), 2));
    }

    @Test
    void reservar_VueloDeVueltaDesdeLaCiudadDelEvento_Descuenta() {
        Vuelo vuelta = vueloEnRepo(ciudadEvento, buenosAires, "2026-11-09T10:00:00Z");
        when(vueloRepository.descontarStock(vuelta.getIdVuelo(), 1)).thenReturn(1);

        assertSame(vuelta, flightService.reservar(ciudadEvento.getIdCiudad(), vuelta.getIdVuelo(), 1));
    }

    @Test
    void reservar_VueloAjenoALaCiudadDelEvento_LanzaIllegalArgument() {
        Ciudad otra = new Ciudad(UUID.randomUUID(), "Madrid", null, null);
        Vuelo ajeno = vueloEnRepo(buenosAires, otra, "2026-11-05T10:00:00Z");

        assertThrows(IllegalArgumentException.class, () -> flightService.reservar(ciudadEvento.getIdCiudad(), ajeno.getIdVuelo(), 1));
        verify(vueloRepository, never()).descontarStock(any(), anyInt());
    }

    @Test
    void reservar_VueloYaPartido_LanzaIllegalArgument() {
        Vuelo partido = vueloEnRepo(buenosAires, ciudadEvento, "2026-09-30T10:00:00Z");

        assertThrows(IllegalArgumentException.class, () -> flightService.reservar(ciudadEvento.getIdCiudad(), partido.getIdVuelo(), 1));
        verify(vueloRepository, never()).descontarStock(any(), anyInt());
    }

    @Test
    void reservar_SinAsientos_LanzaStockInsuficiente() {
        Vuelo ida = vueloEnRepo(buenosAires, ciudadEvento, "2026-11-05T10:00:00Z");
        when(vueloRepository.descontarStock(ida.getIdVuelo(), 11)).thenReturn(0);

        assertThrows(StockInsuficienteException.class, () -> flightService.reservar(ciudadEvento.getIdCiudad(), ida.getIdVuelo(), 11));
    }
}
