package com.uade.grandprixtracker.hotel.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "hoteles")
public class Hotel {

    @Id
    @Column(name = "id_hotel", nullable = false, updatable = false)
    private UUID idHotel;

    @Column(name = "id_ciudad", nullable = false)
    private UUID idCiudad;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "estrellas")
    private Integer estrellas;

    @Column(name = "distancia_circuito_km")
    private BigDecimal distanciaCircuitoKm;

    @Column(name = "ofrece_traslado")
    private Boolean ofreceTraslado;

    @Column(name = "imagen_principal_url", columnDefinition = "text")
    private String imagenPrincipalUrl;

    public Hotel() {
    }

    public Hotel(UUID idHotel, UUID idCiudad, String nombre) {
        this.idHotel = idHotel;
        this.idCiudad = idCiudad;
        this.nombre = nombre;
    }

    public UUID getIdHotel() {
        return idHotel;
    }

    public UUID getIdCiudad() {
        return idCiudad;
    }

    public String getNombre() {
        return nombre;
    }

    public Integer getEstrellas() {
        return estrellas;
    }

    public BigDecimal getDistanciaCircuitoKm() {
        return distanciaCircuitoKm;
    }

    public Boolean getOfreceTraslado() {
        return ofreceTraslado;
    }

    public String getImagenPrincipalUrl() {
        return imagenPrincipalUrl;
    }
}
