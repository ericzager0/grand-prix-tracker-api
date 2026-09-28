package com.uade.grandprixtracker.event.soap.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CiudadSoapDto", namespace = "http://uade.com/grandprixtracker/soap/events")
public class CiudadSoapDto {

    @XmlElement(name = "idCiudad")
    private String idCiudad;

    @XmlElement(name = "nombre", required = true)
    private String nombre;

    @XmlElement(name = "pais")
    private PaisSoapDto pais;

    public CiudadSoapDto() {}

    public CiudadSoapDto(String idCiudad, String nombre, PaisSoapDto pais) {
        this.idCiudad = idCiudad;
        this.nombre = nombre;
        this.pais = pais;
    }

    public String getIdCiudad() {
        return idCiudad;
    }

    public void setIdCiudad(String idCiudad) {
        this.idCiudad = idCiudad;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public PaisSoapDto getPais() {
        return pais;
    }

    public void setPais(PaisSoapDto pais) {
        this.pais = pais;
    }
}
