package com.uade.grandprixtracker.booking.model;

import com.uade.grandprixtracker.flight.model.Vuelo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "reserva_detalle_vuelos")
public class ReservaDetalleVuelo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_detalle_vuelo", nullable = false, updatable = false)
    private UUID idDetalleVuelo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_reserva", nullable = false)
    private Reserva reserva;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_vuelo", nullable = false)
    private Vuelo vuelo;

    @Column(name = "cantidad_pasajeros", nullable = false)
    private Integer cantidadPasajeros;

    @Column(name = "subtotal_usd", nullable = false)
    private BigDecimal subtotalUsd;

    public ReservaDetalleVuelo() {
    }

    public ReservaDetalleVuelo(Vuelo vuelo, Integer cantidadPasajeros, BigDecimal subtotalUsd) {
        this.vuelo = vuelo;
        this.cantidadPasajeros = cantidadPasajeros;
        this.subtotalUsd = subtotalUsd;
    }

    void setReserva(Reserva reserva) {
        this.reserva = reserva;
    }

    public Vuelo getVuelo() {
        return vuelo;
    }

    public Integer getCantidadPasajeros() {
        return cantidadPasajeros;
    }

    public BigDecimal getSubtotalUsd() {
        return subtotalUsd;
    }
}
