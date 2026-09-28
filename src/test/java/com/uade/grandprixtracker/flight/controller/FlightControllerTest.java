package com.uade.grandprixtracker.flight.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.uade.grandprixtracker.flight.dto.VueloResponseDto;
import com.uade.grandprixtracker.flight.dto.VueloResponseDto.CiudadResumen;
import com.uade.grandprixtracker.flight.service.FlightService;
import com.uade.grandprixtracker.shared.exception.GlobalExceptionHandler;
import com.uade.grandprixtracker.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class FlightControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FlightService flightService;

    @InjectMocks
    private FlightController flightController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(flightController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getFlightsByEvent_Devuelve200ConSentido() throws Exception {
        UUID idEvento = UUID.randomUUID();
        when(flightService.listarPorEvento(idEvento)).thenReturn(List.of(new VueloResponseDto(
                UUID.randomUUID(), "LATAM", VueloResponseDto.Sentido.IDA,
                new CiudadResumen(UUID.randomUUID(), "Buenos Aires"), new CiudadResumen(UUID.randomUUID(), "São Paulo"),
                OffsetDateTime.parse("2026-11-05T12:00:00Z"), OffsetDateTime.parse("2026-11-05T15:00:00Z"),
                new BigDecimal("320.00"), 60)));

        mockMvc.perform(get("/events/" + idEvento + "/flights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].sentido").value("IDA"))
                .andExpect(jsonPath("$.data[0].origen.nombre").value("Buenos Aires"))
                .andExpect(jsonPath("$.data[0].destino.nombre").value("São Paulo"));
    }

    @Test
    void getFlightsByEvent_EventoInexistente_Devuelve404() throws Exception {
        UUID idEvento = UUID.randomUUID();
        when(flightService.listarPorEvento(idEvento)).thenThrow(new ResourceNotFoundException("Evento", "id", idEvento));

        mockMvc.perform(get("/events/" + idEvento + "/flights"))
                .andExpect(status().isNotFound());
    }
}
