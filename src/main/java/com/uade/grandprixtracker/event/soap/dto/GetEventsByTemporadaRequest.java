package com.uade.grandprixtracker.event.soap.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "getEventsByTemporadaRequest", namespace = "http://uade.com/grandprixtracker/soap/events")
@XmlAccessorType(XmlAccessType.FIELD)
public class GetEventsByTemporadaRequest {

    @XmlElement(name = "temporada", required = true)
    private int temporada;

    public GetEventsByTemporadaRequest() {}

    public GetEventsByTemporadaRequest(int temporada) {
        this.temporada = temporada;
    }

    public int getTemporada() {
        return temporada;
    }

    public void setTemporada(int temporada) {
        this.temporada = temporada;
    }
}
