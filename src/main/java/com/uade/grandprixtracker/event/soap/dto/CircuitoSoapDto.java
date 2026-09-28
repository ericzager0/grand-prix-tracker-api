package com.uade.grandprixtracker.event.soap.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CircuitoSoapDto", namespace = "http://uade.com/grandprixtracker/soap/events")
public class CircuitoSoapDto {

    @XmlElement(name = "idCircuito")
    private String idCircuito;

    @XmlElement(name = "nombre", required = true)
    private String nombre;

    @XmlElement(name = "longitudKm")
    private Double longitudKm;

    @XmlElement(name = "curvas")
    private Integer curvas;

    @XmlElement(name = "vueltas")
    private Integer vueltas;

    @XmlElement(name = "mapaSvgUrl")
    private String mapaSvgUrl;

    @XmlElement(name = "ciudad")
    private CiudadSoapDto ciudad;

    public CircuitoSoapDto() {}

    public CircuitoSoapDto(String idCircuito, String nombre, Double longitudKm, Integer curvas, Integer vueltas, String mapaSvgUrl, CiudadSoapDto ciudad) {
        this.idCircuito = idCircuito;
        this.nombre = nombre;
        this.longitudKm = longitudKm;
        this.curvas = curvas;
        this.vueltas = vueltas;
        this.mapaSvgUrl = mapaSvgUrl;
        this.ciudad = ciudad;
    }

    public String getIdCircuito() {
        return idCircuito;
    }

    public void setIdCircuito(String idCircuito) {
        this.idCircuito = idCircuito;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Double getLongitudKm() {
        return longitudKm;
    }

    public void setLongitudKm(Double longitudKm) {
        this.longitudKm = longitudKm;
    }

    public Integer getCurvas() {
        return curvas;
    }

    public void setCurvas(Integer curvas) {
        this.curvas = curvas;
    }

    public Integer getVueltas() {
        return vueltas;
    }

    public void setVueltas(Integer vueltas) {
        this.vueltas = vueltas;
    }

    public String getMapaSvgUrl() {
        return mapaSvgUrl;
    }

    public void setMapaSvgUrl(String mapaSvgUrl) {
        this.mapaSvgUrl = mapaSvgUrl;
    }

    public CiudadSoapDto getCiudad() {
        return ciudad;
    }

    public void setCiudad(CiudadSoapDto ciudad) {
        this.ciudad = ciudad;
    }
}
