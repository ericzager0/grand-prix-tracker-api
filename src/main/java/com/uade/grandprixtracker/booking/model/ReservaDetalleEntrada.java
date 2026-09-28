package com.uade.grandprixtracker.booking.model;

import com.uade.grandprixtracker.ticket.model.EntradaGrada;
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
@Table(name = "reserva_detalle_entradas")
public class ReservaDetalleEntrada {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_detalle_entrada", nullable = false, updatable = false)
    private UUID idDetalleEntrada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_reserva", nullable = false)
    private Reserva reserva;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_entrada", nullable = false)
    private EntradaGrada entrada;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "subtotal_usd", nullable = false)
    private BigDecimal subtotalUsd;

    public ReservaDetalleEntrada() {
    }

    public ReservaDetalleEntrada(EntradaGrada entrada, Integer cantidad, BigDecimal subtotalUsd) {
        this.entrada = entrada;
        this.cantidad = cantidad;
        this.subtotalUsd = subtotalUsd;
    }

    void setReserva(Reserva reserva) {
        this.reserva = reserva;
    }

    public EntradaGrada getEntrada() {
        return entrada;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public BigDecimal getSubtotalUsd() {
        return subtotalUsd;
    }
}
