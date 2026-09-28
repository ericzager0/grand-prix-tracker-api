package com.uade.grandprixtracker.hotel.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.uade.grandprixtracker.event.model.Circuito;
import com.uade.grandprixtracker.event.model.Ciudad;
import com.uade.grandprixtracker.event.model.EventoF1;
import com.uade.grandprixtracker.event.repository.EventoF1Repository;
import com.uade.grandprixtracker.hotel.dto.HotelResponseDto;
import com.uade.grandprixtracker.hotel.model.HabitacionHotel;
import com.uade.grandprixtracker.hotel.model.Hotel;
import com.uade.grandprixtracker.hotel.repository.HabitacionHotelRepository;
import com.uade.grandprixtracker.shared.exception.ResourceNotFoundException;
import com.uade.grandprixtracker.shared.exception.StockInsuficienteException;
import java.math.BigDecimal;
import java.time.LocalDate;
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
class HotelServiceTest {

    @Mock
    private HabitacionHotelRepository habitacionHotelRepository;

    @Mock
    private EventoF1Repository eventoF1Repository;

    @InjectMocks
    private HotelService hotelService;

    private UUID idCiudad;
    private Hotel hotel;
    private HabitacionHotel habitacion;

    @BeforeEach
    void setUp() {
        idCiudad = UUID.randomUUID();
        hotel = new Hotel(UUID.randomUUID(), idCiudad, "Hotel Paulista");
        habitacion = new HabitacionHotel(UUID.randomUUID(), hotel, "Doble", new BigDecimal("200.00"), 1);
    }

    @Test
    void listarPorEvento_AgrupaHabitacionesPorHotelRespetandoElOrden() {
        UUID idEvento = UUID.randomUUID();
        Ciudad ciudad = new Ciudad(idCiudad, "São Paulo", null, null);
        Circuito circuito = new Circuito(UUID.randomUUID(), "Interlagos", null, null, null, null, ciudad, null);
        EventoF1 evento = new EventoF1(idEvento, 2026, LocalDate.of(2026, 11, 6), LocalDate.of(2026, 11, 8), "Proximo", circuito, null);
        HabitacionHotel suite = new HabitacionHotel(UUID.randomUUID(), hotel, "Suite", new BigDecimal("450.00"), 2);
        Hotel otroHotel = new Hotel(UUID.randomUUID(), idCiudad, "Paulista Grand");
        HabitacionHotel otraDoble = new HabitacionHotel(UUID.randomUUID(), otroHotel, "Doble", new BigDecimal("320.00"), 5);
        when(eventoF1Repository.findById(idEvento)).thenReturn(Optional.of(evento));
        when(habitacionHotelRepository.findByCiudadWithHotel(idCiudad)).thenReturn(List.of(habitacion, suite, otraDoble));

        List<HotelResponseDto> hoteles = hotelService.listarPorEvento(idEvento);

        assertEquals(2, hoteles.size());
        assertEquals("Hotel Paulista", hoteles.get(0).nombre());
        assertEquals(2, hoteles.get(0).habitaciones().size());
        assertEquals("Paulista Grand", hoteles.get(1).nombre());
        assertEquals(1, hoteles.get(1).habitaciones().size());
    }

    @Test
    void listarPorEvento_EventoInexistente_LanzaNotFound() {
        UUID idEvento = UUID.randomUUID();
        when(eventoF1Repository.findById(idEvento)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> hotelService.listarPorEvento(idEvento));
    }

    @Test
    void reservar_MismaCiudadConStock_DescuentaUnaHabitacion() {
        when(habitacionHotelRepository.findByIdWithHotel(habitacion.getIdHabitacion())).thenReturn(Optional.of(habitacion));
        when(habitacionHotelRepository.descontarStock(habitacion.getIdHabitacion())).thenReturn(1);

        assertSame(habitacion, hotelService.reservar(idCiudad, habitacion.getIdHabitacion()));
    }

    @Test
    void reservar_HotelEnOtraCiudad_LanzaIllegalArgumentSinDescontar() {
        when(habitacionHotelRepository.findByIdWithHotel(habitacion.getIdHabitacion())).thenReturn(Optional.of(habitacion));

        assertThrows(IllegalArgumentException.class,
                () -> hotelService.reservar(UUID.randomUUID(), habitacion.getIdHabitacion()));
        verify(habitacionHotelRepository, never()).descontarStock(any());
    }

    @Test
    void reservar_SinStock_LanzaStockInsuficiente() {
        when(habitacionHotelRepository.findByIdWithHotel(habitacion.getIdHabitacion())).thenReturn(Optional.of(habitacion));
        when(habitacionHotelRepository.descontarStock(habitacion.getIdHabitacion())).thenReturn(0);

        assertThrows(StockInsuficienteException.class,
                () -> hotelService.reservar(idCiudad, habitacion.getIdHabitacion()));
    }
}
