package com.uade.grandprixtracker.hotel.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "habitaciones_hotel")
public class HabitacionHotel {

    @Id
    @Column(name = "id_habitacion", nullable = false, updatable = false)
    private UUID idHabitacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_hotel", nullable = false)
    private Hotel hotel;

    @Column(name = "tipo", nullable = false)
    private String tipo;

    @Column(name = "precio_por_noche_usd", nullable = false)
    private BigDecimal precioPorNocheUsd;

    @Column(name = "stock_disponible", nullable = false)
    private Integer stockDisponible;

    public HabitacionHotel() {
    }

    public HabitacionHotel(UUID idHabitacion, Hotel hotel, String tipo, BigDecimal precioPorNocheUsd, Integer stockDisponible) {
        this.idHabitacion = idHabitacion;
        this.hotel = hotel;
        this.tipo = tipo;
        this.precioPorNocheUsd = precioPorNocheUsd;
        this.stockDisponible = stockDisponible;
    }

    public UUID getIdHabitacion() {
        return idHabitacion;
    }

    public Hotel getHotel() {
        return hotel;
    }

    public String getTipo() {
        return tipo;
    }

    public BigDecimal getPrecioPorNocheUsd() {
        return precioPorNocheUsd;
    }

    public Integer getStockDisponible() {
        return stockDisponible;
    }
}
