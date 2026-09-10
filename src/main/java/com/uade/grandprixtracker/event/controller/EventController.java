package com.uade.grandprixtracker.event.controller;

import com.uade.grandprixtracker.event.dto.EventoF1ResponseDto;
import com.uade.grandprixtracker.event.service.EventService;
import com.uade.grandprixtracker.shared.response.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EventoF1ResponseDto>>> getAllEvents(
            @RequestParam(name = "temporada", required = false) Integer temporada) {
        List<EventoF1ResponseDto> events = (temporada != null)
                ? eventService.getEventsByTemporada(temporada)
                : eventService.getAllEvents();
        return ResponseEntity.ok(ApiResponse.success("Eventos obtenidos correctamente", events));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventoF1ResponseDto>> getEventById(@PathVariable UUID id) {
        EventoF1ResponseDto event = eventService.getEventById(id);
        return ResponseEntity.ok(ApiResponse.success("Evento obtenido correctamente", event));
    }
}

