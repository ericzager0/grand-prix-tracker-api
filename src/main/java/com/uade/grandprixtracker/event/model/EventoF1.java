package com.uade.grandprixtracker.event.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "eventos_f1")
public class EventoF1 {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_evento", nullable = false, updatable = false)
    private UUID idEvento;

    @Column(name = "temporada", nullable = false)
    private Integer temporada;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "estado")
    private String estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_circuito", nullable = false)
    private Circuito circuito;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public EventoF1() {
    }

    public EventoF1(UUID idEvento, Integer temporada, LocalDate fechaInicio, LocalDate fechaFin, String estado, Circuito circuito, OffsetDateTime createdAt) {
        this.idEvento = idEvento;
        this.temporada = temporada;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.estado = estado;
        this.circuito = circuito;
        this.createdAt = createdAt;
    }

    public UUID getIdEvento() {
        return idEvento;
    }

    public void setIdEvento(UUID idEvento) {
        this.idEvento = idEvento;
    }

    public Integer getTemporada() {
        return temporada;
    }

    public void setTemporada(Integer temporada) {
        this.temporada = temporada;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Circuito getCircuito() {
        return circuito;
    }

    public void setCircuito(Circuito circuito) {
        this.circuito = circuito;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

