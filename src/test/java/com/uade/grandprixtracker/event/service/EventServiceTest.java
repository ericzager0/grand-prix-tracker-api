package com.uade.grandprixtracker.event.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.uade.grandprixtracker.event.dto.EventoF1ResponseDto;
import com.uade.grandprixtracker.event.model.Circuito;
import com.uade.grandprixtracker.event.model.Ciudad;
import com.uade.grandprixtracker.event.model.EventoF1;
import com.uade.grandprixtracker.event.model.Pais;
import com.uade.grandprixtracker.event.repository.EventoF1Repository;
import com.uade.grandprixtracker.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventoF1Repository eventoF1Repository;

    @InjectMocks
    private EventService eventService;

    private EventoF1 testEvento;
    private UUID eventoId;

    @BeforeEach
    void setUp() {
        eventoId = UUID.randomUUID();
        UUID circuitoId = UUID.randomUUID();
        UUID ciudadId = UUID.randomUUID();
        UUID paisId = UUID.randomUUID();

        Pais pais = new Pais(paisId, "Argentina", "ARG", "latin-america", OffsetDateTime.now());
        Ciudad ciudad = new Ciudad(ciudadId, "Buenos Aires", pais, OffsetDateTime.now());
        Circuito circuito = new Circuito(
                circuitoId,
                "Autódromo Oscar y Juan Gálvez",
                new BigDecimal("4.259"),
                15,
                72,
                "https://example.com/galvez.svg",
                ciudad,
                OffsetDateTime.now()
        );

        testEvento = new EventoF1(
                eventoId,
                2026,
                LocalDate.of(2026, 3, 20),
                LocalDate.of(2026, 3, 22),
                "Proximo",
                circuito,
                OffsetDateTime.now()
        );
    }

    @Test
    void testGetAllEvents() {
        when(eventoF1Repository.findAllWithCircuitCityCountry()).thenReturn(List.of(testEvento));

        List<EventoF1ResponseDto> results = eventService.getAllEvents();

        assertNotNull(results);
        assertEquals(1, results.size());

        EventoF1ResponseDto dto = results.getFirst();
        assertEquals(eventoId, dto.idEvento());
        assertEquals(2026, dto.temporada());
        assertEquals("Proximo", dto.estado());
        assertEquals(LocalDate.of(2026, 3, 20), dto.fechaInicio());

        // Validar circuito
        assertNotNull(dto.circuito());
        assertEquals("Autódromo Oscar y Juan Gálvez", dto.circuito().nombre());
        assertEquals(new BigDecimal("4.259"), dto.circuito().longitudKm());
        assertEquals(15, dto.circuito().curvas());
        assertEquals(72, dto.circuito().vueltas());

        // Validar ciudad
        assertNotNull(dto.circuito().ciudad());
        assertEquals("Buenos Aires", dto.circuito().ciudad().nombre());

        // Validar país
        assertNotNull(dto.circuito().ciudad().pais());
        assertEquals("Argentina", dto.circuito().ciudad().pais().nombre());
        assertEquals("ARG", dto.circuito().ciudad().pais().codigoIso());
        assertEquals("latin-america", dto.circuito().ciudad().pais().continente());
    }

    @Test
    void testGetEventById_Found() {
        when(eventoF1Repository.findByIdWithCircuitCityCountry(eventoId)).thenReturn(Optional.of(testEvento));

        EventoF1ResponseDto dto = eventService.getEventById(eventoId);

        assertNotNull(dto);
        assertEquals(eventoId, dto.idEvento());
        assertEquals("Argentina", dto.circuito().ciudad().pais().nombre());
    }

    @Test
    void testGetEventById_NotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(eventoF1Repository.findByIdWithCircuitCityCountry(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> eventService.getEventById(nonExistentId));
    }
}

