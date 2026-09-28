package com.uade.grandprixtracker.event.soap;

import com.uade.grandprixtracker.event.dto.EventoF1ResponseDto;
import com.uade.grandprixtracker.event.service.EventService;
import com.uade.grandprixtracker.event.soap.dto.EventoSoapDto;
import com.uade.grandprixtracker.event.soap.dto.GetAllEventsRequest;
import com.uade.grandprixtracker.event.soap.dto.GetAllEventsResponse;
import com.uade.grandprixtracker.event.soap.dto.GetEventByIdRequest;
import com.uade.grandprixtracker.event.soap.dto.GetEventByIdResponse;
import com.uade.grandprixtracker.event.soap.dto.GetEventsByTemporadaRequest;
import com.uade.grandprixtracker.event.soap.dto.GetEventsByTemporadaResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
public class EventSoapEndpoint {

    public static final String NAMESPACE_URI = "http://uade.com/grandprixtracker/soap/events";

    private final EventService eventService;
    private final EventSoapMapper mapper;

    public EventSoapEndpoint(EventService eventService, EventSoapMapper mapper) {
        this.eventService = eventService;
        this.mapper = mapper;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getEventByIdRequest")
    @ResponsePayload
    public GetEventByIdResponse getEventById(@RequestPayload GetEventByIdRequest request) {
        UUID id = UUID.fromString(request.getId());
        EventoF1ResponseDto dto = eventService.getEventById(id);
        return new GetEventByIdResponse(mapper.toSoapDto(dto));
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getEventsByTemporadaRequest")
    @ResponsePayload
    public GetEventsByTemporadaResponse getEventsByTemporada(@RequestPayload GetEventsByTemporadaRequest request) {
        List<EventoSoapDto> eventos = eventService.getEventsByTemporada(request.getTemporada())
                .stream()
                .map(mapper::toSoapDto)
                .toList();
        return new GetEventsByTemporadaResponse(eventos);
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getAllEventsRequest")
    @ResponsePayload
    public GetAllEventsResponse getAllEvents(@RequestPayload GetAllEventsRequest request) {
        List<EventoSoapDto> eventos = eventService.getAllEvents()
                .stream()
                .map(mapper::toSoapDto)
                .toList();
        return new GetAllEventsResponse(eventos);
    }
}
