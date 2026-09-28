package com.uade.grandprixtracker.ticket.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "entradas_gradas")
public class EntradaGrada {

    @Id
    @Column(name = "id_entrada", nullable = false, updatable = false)
    private UUID idEntrada;

    @Column(name = "id_evento", nullable = false)
    private UUID idEvento;

    @Column(name = "nombre_tribuna", nullable = false)
    private String nombreTribuna;

    @Column(name = "precio_usd", nullable = false)
    private BigDecimal precioUsd;

    @Column(name = "stock_disponible", nullable = false)
    private Integer stockDisponible;

    @Column(name = "tipo")
    private String tipo;

    public EntradaGrada() {
    }

    public EntradaGrada(UUID idEntrada, UUID idEvento, String nombreTribuna, BigDecimal precioUsd, Integer stockDisponible, String tipo) {
        this.idEntrada = idEntrada;
        this.idEvento = idEvento;
        this.nombreTribuna = nombreTribuna;
        this.precioUsd = precioUsd;
        this.stockDisponible = stockDisponible;
        this.tipo = tipo;
    }

    public UUID getIdEntrada() {
        return idEntrada;
    }

    public UUID getIdEvento() {
        return idEvento;
    }

    public String getNombreTribuna() {
        return nombreTribuna;
    }

    public BigDecimal getPrecioUsd() {
        return precioUsd;
    }

    public Integer getStockDisponible() {
        return stockDisponible;
    }

    public String getTipo() {
        return tipo;
    }
}
