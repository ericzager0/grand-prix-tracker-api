package com.uade.grandprixtracker.event.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "ciudades",
    indexes = {
        @Index(name = "idx_ciudades_id_pais", columnList = "id_pais")
    }
)
public class Ciudad {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_ciudad", nullable = false, updatable = false)
    private UUID idCiudad;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pais", nullable = false)
    private Pais pais;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public Ciudad() {
    }

    public Ciudad(UUID idCiudad, String nombre, Pais pais, OffsetDateTime createdAt) {
        this.idCiudad = idCiudad;
        this.nombre = nombre;
        this.pais = pais;
        this.createdAt = createdAt;
    }

    public UUID getIdCiudad() {
        return idCiudad;
    }

    public void setIdCiudad(UUID idCiudad) {
        this.idCiudad = idCiudad;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Pais getPais() {
        return pais;
    }

    public void setPais(Pais pais) {
        this.pais = pais;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

