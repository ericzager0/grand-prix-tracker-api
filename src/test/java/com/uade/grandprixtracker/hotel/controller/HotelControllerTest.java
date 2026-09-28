package com.uade.grandprixtracker.hotel.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.uade.grandprixtracker.hotel.dto.HotelResponseDto;
import com.uade.grandprixtracker.hotel.service.HotelService;
import com.uade.grandprixtracker.shared.exception.GlobalExceptionHandler;
import com.uade.grandprixtracker.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
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
class HotelControllerTest {

    private MockMvc mockMvc;

    @Mock
    private HotelService hotelService;

    @InjectMocks
    private HotelController hotelController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(hotelController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getHotelsByEvent_Devuelve200ConHotelesYHabitaciones() throws Exception {
        UUID idEvento = UUID.randomUUID();
        when(hotelService.listarPorEvento(idEvento)).thenReturn(List.of(new HotelResponseDto(
                UUID.randomUUID(), "Interlagos Plaza Hotel", 4, new BigDecimal("3.1"), true, null,
                List.of(new HotelResponseDto.HabitacionResponseDto(UUID.randomUUID(), "Doble", new BigDecimal("190.00"), 20)))));

        mockMvc.perform(get("/events/" + idEvento + "/hotels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].nombre").value("Interlagos Plaza Hotel"))
                .andExpect(jsonPath("$.data[0].ofreceTraslado").value(true))
                .andExpect(jsonPath("$.data[0].habitaciones[0].tipo").value("Doble"));
    }

    @Test
    void getHotelsByEvent_EventoInexistente_Devuelve404() throws Exception {
        UUID idEvento = UUID.randomUUID();
        when(hotelService.listarPorEvento(idEvento)).thenThrow(new ResourceNotFoundException("Evento", "id", idEvento));

        mockMvc.perform(get("/events/" + idEvento + "/hotels"))
                .andExpect(status().isNotFound());
    }
}
