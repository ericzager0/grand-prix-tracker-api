package com.uade.grandprixtracker.event.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.uade.grandprixtracker.event.dto.CircuitoResponseDto;
import com.uade.grandprixtracker.event.dto.CiudadResponseDto;
import com.uade.grandprixtracker.event.dto.EventoF1ResponseDto;
import com.uade.grandprixtracker.event.dto.PaisResponseDto;
import com.uade.grandprixtracker.event.service.EventService;
import com.uade.grandprixtracker.shared.exception.GlobalExceptionHandler;
import com.uade.grandprixtracker.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EventService eventService;

    @InjectMocks
    private EventController eventController;

    private UUID eventoId;
    private EventoF1ResponseDto sampleDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(eventController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        eventoId = UUID.randomUUID();
        UUID circuitoId = UUID.randomUUID();
        UUID ciudadId = UUID.randomUUID();
        UUID paisId = UUID.randomUUID();

        PaisResponseDto pais = new PaisResponseDto(paisId, "Italia", "ITA", "europe");
        CiudadResponseDto ciudad = new CiudadResponseDto(ciudadId, "Monza", pais);
        CircuitoResponseDto circuito = new CircuitoResponseDto(
                circuitoId,
                "Autodromo Nazionale Monza",
                new BigDecimal("5.793"),
                11,
                53,
                "https://example.com/monza.svg",
                ciudad
        );

        sampleDto = new EventoF1ResponseDto(
                eventoId,
                2026,
                LocalDate.of(2026, 9, 4),
                LocalDate.of(2026, 9, 6),
                "Proximo",
                circuito
        );
    }

    @Test
    void testGetAllEvents_ReturnsListWithFullDetails() throws Exception {
        when(eventService.getAllEvents()).thenReturn(List.of(sampleDto));

        mockMvc.perform(get("/events")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].idEvento").value(eventoId.toString()))
                .andExpect(jsonPath("$.data[0].temporada").value(2026))
                .andExpect(jsonPath("$.data[0].circuito.nombre").value("Autodromo Nazionale Monza"))
                .andExpect(jsonPath("$.data[0].circuito.ciudad.nombre").value("Monza"))
                .andExpect(jsonPath("$.data[0].circuito.ciudad.pais.nombre").value("Italia"))
                .andExpect(jsonPath("$.data[0].circuito.ciudad.pais.continente").value("europe"));

        verify(eventService, times(1)).getAllEvents();
    }

    @Test
    void testGetEventById_Success() throws Exception {
        when(eventService.getEventById(eventoId)).thenReturn(sampleDto);

        mockMvc.perform(get("/events/" + eventoId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.idEvento").value(eventoId.toString()))
                .andExpect(jsonPath("$.data.circuito.nombre").value("Autodromo Nazionale Monza"));

        verify(eventService, times(1)).getEventById(eventoId);
    }

    @Test
    void testGetEventById_NotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        when(eventService.getEventById(nonExistentId))
                .thenThrow(new ResourceNotFoundException("Evento", "id", nonExistentId));

        mockMvc.perform(get("/events/" + nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        verify(eventService, times(1)).getEventById(nonExistentId);
    }

    @Test
    void testGetAllEvents_WithTemporadaParam() throws Exception {
        when(eventService.getEventsByTemporada(2026)).thenReturn(List.of(sampleDto));

        mockMvc.perform(get("/events?temporada=2026")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].temporada").value(2026));

        verify(eventService, times(1)).getEventsByTemporada(2026);
        verify(eventService, never()).getAllEvents();
    }
}

