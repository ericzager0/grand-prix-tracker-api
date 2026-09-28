package com.uade.grandprixtracker.event.soap.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PaisSoapDto", namespace = "http://uade.com/grandprixtracker/soap/events")
public class PaisSoapDto {

    @XmlElement(name = "idPais")
    private String idPais;

    @XmlElement(name = "nombre", required = true)
    private String nombre;

    @XmlElement(name = "codigoIso")
    private String codigoIso;

    @XmlElement(name = "continente")
    private String continente;

    public PaisSoapDto() {}

    public PaisSoapDto(String idPais, String nombre, String codigoIso, String continente) {
        this.idPais = idPais;
        this.nombre = nombre;
        this.codigoIso = codigoIso;
        this.continente = continente;
    }

    public String getIdPais() {
        return idPais;
    }

    public void setIdPais(String idPais) {
        this.idPais = idPais;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCodigoIso() {
        return codigoIso;
    }

    public void setCodigoIso(String codigoIso) {
        this.codigoIso = codigoIso;
    }

    public String getContinente() {
        return continente;
    }

    public void setContinente(String continente) {
        this.continente = continente;
    }
}
