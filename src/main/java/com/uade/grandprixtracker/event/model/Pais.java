package com.uade.grandprixtracker.event.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "paises")
public class Pais {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_pais", nullable = false, updatable = false)
    private UUID idPais;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "codigo_iso", nullable = false, unique = true)
    private String codigoIso;

    @Column(name = "continente", nullable = false)
    private String continente;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public Pais() {
    }

    public Pais(UUID idPais, String nombre, String codigoIso, String continente, OffsetDateTime createdAt) {
        this.idPais = idPais;
        this.nombre = nombre;
        this.codigoIso = codigoIso;
        this.continente = continente;
        this.createdAt = createdAt;
    }

    public UUID getIdPais() {
        return idPais;
    }

    public void setIdPais(UUID idPais) {
        this.idPais = idPais;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCodigoIso() {
        return codigoIso;
    }

    public void setCodigoIso(String codigoIso) {
        this.codigoIso = codigoIso;
    }

    public String getContinente() {
        return continente;
    }

    public void setContinente(String continente) {
        this.continente = continente;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

