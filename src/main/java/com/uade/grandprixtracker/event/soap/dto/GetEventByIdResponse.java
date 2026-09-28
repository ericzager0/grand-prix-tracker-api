package com.uade.grandprixtracker.event.soap.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "getEventByIdResponse", namespace = "http://uade.com/grandprixtracker/soap/events")
@XmlAccessorType(XmlAccessType.FIELD)
public class GetEventByIdResponse {

    @XmlElement(name = "evento")
    private EventoSoapDto evento;

    public GetEventByIdResponse() {}

    public GetEventByIdResponse(EventoSoapDto evento) {
        this.evento = evento;
    }

    public EventoSoapDto getEvento() {
        return evento;
    }

    public void setEvento(EventoSoapDto evento) {
        this.evento = evento;
    }
}
