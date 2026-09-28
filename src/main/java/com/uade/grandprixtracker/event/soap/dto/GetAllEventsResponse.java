package com.uade.grandprixtracker.event.soap.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "getAllEventsResponse", namespace = "http://uade.com/grandprixtracker/soap/events")
@XmlAccessorType(XmlAccessType.FIELD)
public class GetAllEventsResponse {

    @XmlElement(name = "eventos")
    private List<EventoSoapDto> eventos = new ArrayList<>();

    public GetAllEventsResponse() {}

    public GetAllEventsResponse(List<EventoSoapDto> eventos) {
        this.eventos = eventos;
    }

    public List<EventoSoapDto> getEventos() {
        return eventos;
    }

    public void setEventos(List<EventoSoapDto> eventos) {
        this.eventos = eventos;
    }
}
