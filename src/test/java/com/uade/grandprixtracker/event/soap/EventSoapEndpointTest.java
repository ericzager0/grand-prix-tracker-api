package com.uade.grandprixtracker.event.soap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uade.grandprixtracker.event.dto.CircuitoResponseDto;
import com.uade.grandprixtracker.event.dto.CiudadResponseDto;
import com.uade.grandprixtracker.event.dto.EventoF1ResponseDto;
import com.uade.grandprixtracker.event.dto.PaisResponseDto;
import com.uade.grandprixtracker.event.service.EventService;
import com.uade.grandprixtracker.event.soap.dto.GetAllEventsRequest;
import com.uade.grandprixtracker.event.soap.dto.GetAllEventsResponse;
import com.uade.grandprixtracker.event.soap.dto.GetEventByIdRequest;
import com.uade.grandprixtracker.event.soap.dto.GetEventByIdResponse;
import com.uade.grandprixtracker.event.soap.dto.GetEventsByTemporadaRequest;
import com.uade.grandprixtracker.event.soap.dto.GetEventsByTemporadaResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EventSoapEndpointTest {

    private EventService eventService;
    private EventSoapMapper mapper;
    private EventSoapEndpoint endpoint;

    @BeforeEach
    void setUp() {
        eventService = mock(EventService.class);
        mapper = new EventSoapMapper();
        endpoint = new EventSoapEndpoint(eventService, mapper);
    }

    @Test
    @DisplayName("SOAP: getEventById debería retornar el evento mapeado a SOAP DTO")
    void testGetEventById() {
        UUID eventId = UUID.randomUUID();
        UUID circuitId = UUID.randomUUID();
        UUID cityId = UUID.randomUUID();
        UUID countryId = UUID.randomUUID();

        PaisResponseDto pais = new PaisResponseDto(countryId, "Italia", "ITA", "Europa");
        CiudadResponseDto ciudad = new CiudadResponseDto(cityId, "Monza", pais);
        CircuitoResponseDto circuito = new CircuitoResponseDto(circuitId, "Autodromo Nazionale Monza",
                new BigDecimal("5.793"), 11, 53, "http://svg.monza", ciudad);

        EventoF1ResponseDto dto = new EventoF1ResponseDto(
                eventId,
                2026,
                LocalDate.of(2026, 9, 4),
                LocalDate.of(2026, 9, 6),
                "SCHEDULED",
                circuito
        );

        when(eventService.getEventById(eventId)).thenReturn(dto);

        GetEventByIdRequest request = new GetEventByIdRequest(eventId.toString());
        GetEventByIdResponse response = endpoint.getEventById(request);

        assertNotNull(response);
        assertNotNull(response.getEvento());
        assertEquals(eventId.toString(), response.getEvento().getIdEvento());
        assertEquals(2026, response.getEvento().getTemporada());
        assertEquals("Autodromo Nazionale Monza", response.getEvento().getCircuito().getNombre());
        assertEquals("Monza", response.getEvento().getCircuito().getCiudad().getNombre());
        assertEquals("Italia", response.getEvento().getCircuito().getCiudad().getPais().getNombre());

        verify(eventService).getEventById(eventId);
    }

    @Test
    @DisplayName("SOAP: getEventsByTemporada debería retornar la lista para la temporada")
    void testGetEventsByTemporada() {
        EventoF1ResponseDto dto = new EventoF1ResponseDto(
                UUID.randomUUID(),
                2026,
                LocalDate.of(2026, 5, 22),
                LocalDate.of(2026, 5, 24),
                "SCHEDULED",
                null
        );

        when(eventService.getEventsByTemporada(2026)).thenReturn(List.of(dto));

        GetEventsByTemporadaRequest request = new GetEventsByTemporadaRequest(2026);
        GetEventsByTemporadaResponse response = endpoint.getEventsByTemporada(request);

        assertNotNull(response);
        assertEquals(1, response.getEventos().size());
        assertEquals(2026, response.getEventos().get(0).getTemporada());

        verify(eventService).getEventsByTemporada(2026);
    }

    @Test
    @DisplayName("SOAP: getAllEvents debería retornar todos los eventos mapeados")
    void testGetAllEvents() {
        EventoF1ResponseDto dto1 = new EventoF1ResponseDto(
                UUID.randomUUID(), 2025, LocalDate.now(), LocalDate.now(), "FINISHED", null
        );
        EventoF1ResponseDto dto2 = new EventoF1ResponseDto(
                UUID.randomUUID(), 2026, LocalDate.now(), LocalDate.now(), "SCHEDULED", null
        );

        when(eventService.getAllEvents()).thenReturn(List.of(dto1, dto2));

        GetAllEventsRequest request = new GetAllEventsRequest();
        GetAllEventsResponse response = endpoint.getAllEvents(request);

        assertNotNull(response);
        assertEquals(2, response.getEventos().size());

        verify(eventService).getAllEvents();
    }
}
