package com.uade.grandprixtracker.notification.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "notificaciones",
    indexes = {
        @Index(name = "idx_notificaciones_id_usuario", columnList = "id_usuario"),
        @Index(name = "idx_notificaciones_usuario_leido", columnList = "id_usuario, leido"),
        @Index(name = "idx_notificaciones_creado_en", columnList = "creado_en")
    }
)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_notificacion", nullable = false, updatable = false)
    private UUID idNotificacion;

    @Column(name = "id_usuario")
    private UUID idUsuario;

    @Column(name = "titulo", nullable = false, length = 150)
    private String titulo;

    @Column(name = "mensaje", nullable = false, columnDefinition = "text")
    private String mensaje;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 50)
    private NotificationType tipo;

    @Column(name = "leido", nullable = false)
    private Boolean leido = false;

    @Column(name = "url_destino", length = 255)
    private String urlDestino;

    @Column(name = "metadata", columnDefinition = "text")
    private String metadata;

    @Column(name = "creado_en", updatable = false)
    private OffsetDateTime creadoEn;

    public Notification() {
    }

    public Notification(UUID idUsuario, String titulo, String mensaje, NotificationType tipo, String urlDestino, String metadata) {
        this.idUsuario = idUsuario;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.tipo = tipo;
        this.leido = false;
        this.urlDestino = urlDestino;
        this.metadata = metadata;
    }

    @PrePersist
    public void prePersist() {
        if (this.creadoEn == null) {
            this.creadoEn = OffsetDateTime.now();
        }
        if (this.leido == null) {
            this.leido = false;
        }
    }

    public UUID getIdNotificacion() {
        return idNotificacion;
    }

    public void setIdNotificacion(UUID idNotificacion) {
        this.idNotificacion = idNotificacion;
    }

    public UUID getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(UUID idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public NotificationType getTipo() {
        return tipo;
    }

    public void setTipo(NotificationType tipo) {
        this.tipo = tipo;
    }

    public Boolean getLeido() {
        return leido;
    }

    public void setLeido(Boolean leido) {
        this.leido = leido;
    }

    public String getUrlDestino() {
        return urlDestino;
    }

    public void setUrlDestino(String urlDestino) {
        this.urlDestino = urlDestino;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(OffsetDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }
}
