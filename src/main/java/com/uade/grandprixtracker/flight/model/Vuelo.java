package com.uade.grandprixtracker.flight.model;

import com.uade.grandprixtracker.event.model.Ciudad;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "vuelos")
public class Vuelo {

    @Id
    @Column(name = "id_vuelo", nullable = false, updatable = false)
    private UUID idVuelo;

    @Column(name = "aerolinea", nullable = false)
    private String aerolinea;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origen_id_ciudad", nullable = false)
    private Ciudad origen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destino_id_ciudad", nullable = false)
    private Ciudad destino;

    @Column(name = "fecha_salida", nullable = false)
    private OffsetDateTime fechaSalida;

    @Column(name = "fecha_llegada", nullable = false)
    private OffsetDateTime fechaLlegada;

    @Column(name = "precio_usd", nullable = false)
    private BigDecimal precioUsd;

    @Column(name = "stock_asientos", nullable = false)
    private Integer stockAsientos;

    public Vuelo() {
    }

    public Vuelo(UUID idVuelo, String aerolinea, Ciudad origen, Ciudad destino, OffsetDateTime fechaSalida, OffsetDateTime fechaLlegada, BigDecimal precioUsd, Integer stockAsientos) {
        this.idVuelo = idVuelo;
        this.aerolinea = aerolinea;
        this.origen = origen;
        this.destino = destino;
        this.fechaSalida = fechaSalida;
        this.fechaLlegada = fechaLlegada;
        this.precioUsd = precioUsd;
        this.stockAsientos = stockAsientos;
    }

    public UUID getIdVuelo() {
        return idVuelo;
    }

    public String getAerolinea() {
        return aerolinea;
    }

    public Ciudad getOrigen() {
        return origen;
    }

    public Ciudad getDestino() {
        return destino;
    }

    public OffsetDateTime getFechaSalida() {
        return fechaSalida;
    }

    public OffsetDateTime getFechaLlegada() {
        return fechaLlegada;
    }

    public BigDecimal getPrecioUsd() {
        return precioUsd;
    }

    public Integer getStockAsientos() {
        return stockAsientos;
    }
}
