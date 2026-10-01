package com.uade.grandprixtracker.event.service;

import com.uade.grandprixtracker.event.dto.CircuitoResponseDto;
import com.uade.grandprixtracker.event.dto.CiudadResponseDto;
import com.uade.grandprixtracker.event.dto.EventoF1ResponseDto;
import com.uade.grandprixtracker.event.dto.PaisResponseDto;
import com.uade.grandprixtracker.event.model.Circuito;
import com.uade.grandprixtracker.event.model.Ciudad;
import com.uade.grandprixtracker.event.model.EventoF1;
import com.uade.grandprixtracker.event.model.Pais;
import com.uade.grandprixtracker.event.repository.EventoF1Repository;
import com.uade.grandprixtracker.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EventService {

    private final EventoF1Repository eventoF1Repository;

    public EventService(EventoF1Repository eventoF1Repository) {
        this.eventoF1Repository = eventoF1Repository;
    }

    public List<EventoF1ResponseDto> getAllEvents() {
        return eventoF1Repository.findAllWithCircuitCityCountry()
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public List<EventoF1ResponseDto> getEventsByTemporada(Integer temporada) {
        return eventoF1Repository.findByTemporadaWithCircuitCityCountry(temporada)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public EventoF1ResponseDto getEventById(UUID id) {
        return eventoF1Repository.findByIdWithCircuitCityCountry(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Evento", "id", id));
    }

    private EventoF1ResponseDto mapToDto(EventoF1 evento) {
        CircuitoResponseDto circuitoDto = null;
        if (evento.getCircuito() != null) {
            Circuito circuito = evento.getCircuito();
            CiudadResponseDto ciudadDto = null;
            if (circuito.getCiudad() != null) {
                Ciudad ciudad = circuito.getCiudad();
                PaisResponseDto paisDto = null;
                if (ciudad.getPais() != null) {
                    Pais pais = ciudad.getPais();
                    paisDto = new PaisResponseDto(
                            pais.getIdPais(),
                            pais.getNombre(),
                            pais.getCodigoIso(),
                            pais.getContinente()
                    );
                }
                ciudadDto = new CiudadResponseDto(
                        ciudad.getIdCiudad(),
                        ciudad.getNombre(),
                        paisDto
                );
            }
            String svgUrl = (circuito.getCircuitSvgUrl() != null && !circuito.getCircuitSvgUrl().isBlank())
                    ? circuito.getCircuitSvgUrl()
                    : circuito.getMapaSvgUrl();

            circuitoDto = new CircuitoResponseDto(
                    circuito.getIdCircuito(),
                    circuito.getNombre(),
                    circuito.getLongitudKm(),
                    circuito.getCurvas(),
                    circuito.getVueltas(),
                    circuito.getMapaSvgUrl(),
                    ciudadDto,
                    circuito.getRecord(),
                    circuito.getVelocidadMaxima(),
                    circuito.getMaximoGanador(),
                    svgUrl,
                    circuito.getCapacidad()
            );
        }

        return new EventoF1ResponseDto(
                evento.getIdEvento(),
                evento.getTemporada(),
                evento.getFechaInicio(),
                evento.getFechaFin(),
                evento.getEstado(),
                circuitoDto
        );
    }
}

