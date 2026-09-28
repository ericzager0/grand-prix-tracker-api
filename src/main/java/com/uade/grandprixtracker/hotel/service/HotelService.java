package com.uade.grandprixtracker.hotel.service;

import com.uade.grandprixtracker.event.model.EventoF1;
import com.uade.grandprixtracker.event.repository.EventoF1Repository;
import com.uade.grandprixtracker.hotel.dto.HotelResponseDto;
import com.uade.grandprixtracker.hotel.dto.HotelResponseDto.HabitacionResponseDto;
import com.uade.grandprixtracker.hotel.model.HabitacionHotel;
import com.uade.grandprixtracker.hotel.model.Hotel;
import com.uade.grandprixtracker.hotel.repository.HabitacionHotelRepository;
import com.uade.grandprixtracker.shared.exception.ResourceNotFoundException;
import com.uade.grandprixtracker.shared.exception.StockInsuficienteException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HotelService {

    private final HabitacionHotelRepository habitacionHotelRepository;
    private final EventoF1Repository eventoF1Repository;

    public HotelService(HabitacionHotelRepository habitacionHotelRepository, EventoF1Repository eventoF1Repository) {
        this.habitacionHotelRepository = habitacionHotelRepository;
        this.eventoF1Repository = eventoF1Repository;
    }

    @Transactional(readOnly = true)
    public List<HotelResponseDto> listarPorEvento(UUID idEvento) {
        EventoF1 evento = eventoF1Repository.findById(idEvento)
                .orElseThrow(() -> new ResourceNotFoundException("Evento", "id", idEvento));
        UUID idCiudadEvento = evento.getCircuito().getCiudad().getIdCiudad();

        Map<UUID, Hotel> hoteles = new LinkedHashMap<>();
        Map<UUID, List<HabitacionResponseDto>> habitacionesPorHotel = new LinkedHashMap<>();
        for (HabitacionHotel habitacion : habitacionHotelRepository.findByCiudadWithHotel(idCiudadEvento)) {
            Hotel hotel = habitacion.getHotel();
            hoteles.putIfAbsent(hotel.getIdHotel(), hotel);
            habitacionesPorHotel.computeIfAbsent(hotel.getIdHotel(), id -> new ArrayList<>())
                    .add(new HabitacionResponseDto(
                            habitacion.getIdHabitacion(),
                            habitacion.getTipo(),
                            habitacion.getPrecioPorNocheUsd(),
                            habitacion.getStockDisponible()));
        }

        return hoteles.values().stream()
                .map(hotel -> new HotelResponseDto(
                        hotel.getIdHotel(),
                        hotel.getNombre(),
                        hotel.getEstrellas(),
                        hotel.getDistanciaCircuitoKm(),
                        hotel.getOfreceTraslado(),
                        hotel.getImagenPrincipalUrl(),
                        habitacionesPorHotel.get(hotel.getIdHotel())))
                .toList();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public HabitacionHotel reservar(UUID idCiudadEvento, UUID idHabitacion) {
        HabitacionHotel habitacion = habitacionHotelRepository.findByIdWithHotel(idHabitacion)
                .orElseThrow(() -> new ResourceNotFoundException("Habitación", "id", idHabitacion));

        if (!habitacion.getHotel().getIdCiudad().equals(idCiudadEvento)) {
            throw new IllegalArgumentException("El hotel " + habitacion.getHotel().getNombre() + " no está en la ciudad del evento");
        }

        if (habitacionHotelRepository.descontarStock(idHabitacion) == 0) {
            throw new StockInsuficienteException("No hay habitaciones " + habitacion.getTipo() + " disponibles en " + habitacion.getHotel().getNombre());
        }

        return habitacion;
    }
}
