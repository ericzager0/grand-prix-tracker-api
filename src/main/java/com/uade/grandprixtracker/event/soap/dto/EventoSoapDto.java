package com.uade.grandprixtracker.event.soap.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EventoSoapDto", namespace = "http://uade.com/grandprixtracker/soap/events")
public class EventoSoapDto {

    @XmlElement(name = "idEvento", required = true)
    private String idEvento;

    @XmlElement(name = "temporada", required = true)
    private int temporada;

    @XmlElement(name = "fechaInicio", required = true)
    private String fechaInicio;

    @XmlElement(name = "fechaFin", required = true)
    private String fechaFin;

    @XmlElement(name = "estado", required = true)
    private String estado;

    @XmlElement(name = "circuito")
    private CircuitoSoapDto circuito;

    public EventoSoapDto() {}

    public EventoSoapDto(String idEvento, int temporada, String fechaInicio, String fechaFin, String estado, CircuitoSoapDto circuito) {
        this.idEvento = idEvento;
        this.temporada = temporada;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.estado = estado;
        this.circuito = circuito;
    }

    public String getIdEvento() {
        return idEvento;
    }

    public void setIdEvento(String idEvento) {
        this.idEvento = idEvento;
    }

    public int getTemporada() {
        return temporada;
    }

    public void setTemporada(int temporada) {
        this.temporada = temporada;
    }

    public String getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(String fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public String getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(String fechaFin) {
        this.fechaFin = fechaFin;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public CircuitoSoapDto getCircuito() {
        return circuito;
    }

    public void setCircuito(CircuitoSoapDto circuito) {
        this.circuito = circuito;
    }
}
