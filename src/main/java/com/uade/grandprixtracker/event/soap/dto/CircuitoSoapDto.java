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

    @XmlElement(name = "record")
    private String record;

    @XmlElement(name = "velocidadMaxima")
    private String velocidadMaxima;

    @XmlElement(name = "maximoGanador")
    private String maximoGanador;

    @XmlElement(name = "circuitSvgUrl")
    private String circuitSvgUrl;

    @XmlElement(name = "capacidad")
    private String capacidad;

    public CircuitoSoapDto() {}

    public CircuitoSoapDto(String idCircuito, String nombre, Double longitudKm, Integer curvas, Integer vueltas, String mapaSvgUrl, CiudadSoapDto ciudad) {
        this(idCircuito, nombre, longitudKm, curvas, vueltas, mapaSvgUrl, ciudad, null, null, null, null, null);
    }

    public CircuitoSoapDto(String idCircuito, String nombre, Double longitudKm, Integer curvas, Integer vueltas, String mapaSvgUrl, CiudadSoapDto ciudad, String record, String velocidadMaxima, String maximoGanador, String circuitSvgUrl, String capacidad) {
        this.idCircuito = idCircuito;
        this.nombre = nombre;
        this.longitudKm = longitudKm;
        this.curvas = curvas;
        this.vueltas = vueltas;
        this.mapaSvgUrl = mapaSvgUrl;
        this.ciudad = ciudad;
        this.record = record;
        this.velocidadMaxima = velocidadMaxima;
        this.maximoGanador = maximoGanador;
        this.circuitSvgUrl = circuitSvgUrl;
        this.capacidad = capacidad;
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

    public String getRecord() {
        return record;
    }

    public void setRecord(String record) {
        this.record = record;
    }

    public String getVelocidadMaxima() {
        return velocidadMaxima;
    }

    public void setVelocidadMaxima(String velocidadMaxima) {
        this.velocidadMaxima = velocidadMaxima;
    }

    public String getMaximoGanador() {
        return maximoGanador;
    }

    public void setMaximoGanador(String maximoGanador) {
        this.maximoGanador = maximoGanador;
    }

    public String getCircuitSvgUrl() {
        return circuitSvgUrl;
    }

    public void setCircuitSvgUrl(String circuitSvgUrl) {
        this.circuitSvgUrl = circuitSvgUrl;
    }

    public String getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(String capacidad) {
        this.capacidad = capacidad;
    }
}
