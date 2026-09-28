package com.uade.grandprixtracker.flight.service;

import com.uade.grandprixtracker.event.model.Ciudad;
import com.uade.grandprixtracker.event.model.EventoF1;
import com.uade.grandprixtracker.event.repository.EventoF1Repository;
import com.uade.grandprixtracker.flight.dto.VueloResponseDto;
import com.uade.grandprixtracker.flight.dto.VueloResponseDto.CiudadResumen;
import com.uade.grandprixtracker.flight.dto.VueloResponseDto.Sentido;
import com.uade.grandprixtracker.flight.model.Vuelo;
import com.uade.grandprixtracker.flight.repository.VueloRepository;
import com.uade.grandprixtracker.shared.exception.ResourceNotFoundException;
import com.uade.grandprixtracker.shared.exception.StockInsuficienteException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FlightService {

    private final VueloRepository vueloRepository;
    private final EventoF1Repository eventoF1Repository;
    private final Clock clock;

    public FlightService(VueloRepository vueloRepository, EventoF1Repository eventoF1Repository, Clock clock) {
        this.vueloRepository = vueloRepository;
        this.eventoF1Repository = eventoF1Repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<VueloResponseDto> listarPorEvento(UUID idEvento) {
        EventoF1 evento = eventoF1Repository.findById(idEvento)
                .orElseThrow(() -> new ResourceNotFoundException("Evento", "id", idEvento));
        UUID idCiudadEvento = evento.getCircuito().getCiudad().getIdCiudad();

        return vueloRepository.findDisponiblesPorCiudad(idCiudadEvento, OffsetDateTime.now(clock))
                .stream()
                .map(vuelo -> toDto(vuelo, idCiudadEvento))
                .toList();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Vuelo reservar(UUID idCiudadEvento, UUID idVuelo, int cantidadPasajeros) {
        Vuelo vuelo = vueloRepository.findByIdWithCiudades(idVuelo)
                .orElseThrow(() -> new ResourceNotFoundException("Vuelo", "id", idVuelo));

        if (!vuelo.getDestino().getIdCiudad().equals(idCiudadEvento) && !vuelo.getOrigen().getIdCiudad().equals(idCiudadEvento)) {
            throw new IllegalArgumentException("El vuelo " + idVuelo + " no llega ni sale de la ciudad del evento");
        }

        if (!vuelo.getFechaSalida().isAfter(OffsetDateTime.now(clock))) {
            throw new IllegalArgumentException("El vuelo " + idVuelo + " ya partió");
        }

        if (vueloRepository.descontarStock(idVuelo, cantidadPasajeros) == 0) {
            throw new StockInsuficienteException("No hay asientos suficientes en el vuelo de " + vuelo.getAerolinea());
        }

        return vuelo;
    }

    private static VueloResponseDto toDto(Vuelo vuelo, UUID idCiudadEvento) {
        Sentido sentido = vuelo.getDestino().getIdCiudad().equals(idCiudadEvento) ? Sentido.IDA : Sentido.VUELTA;
        return new VueloResponseDto(
                vuelo.getIdVuelo(),
                vuelo.getAerolinea(),
                sentido,
                resumen(vuelo.getOrigen()),
                resumen(vuelo.getDestino()),
                vuelo.getFechaSalida(),
                vuelo.getFechaLlegada(),
                vuelo.getPrecioUsd(),
                vuelo.getStockAsientos()
        );
    }

    private static CiudadResumen resumen(Ciudad ciudad) {
        return new CiudadResumen(ciudad.getIdCiudad(), ciudad.getNombre());
    }
}
