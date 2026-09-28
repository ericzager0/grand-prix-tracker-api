package com.uade.grandprixtracker.notification.model;

/**
 * Define el patrón de distribución de la notificación.
 * - PUB_SUB: Notificación tipo Publicación/Suscripción dirigida a todos los usuarios (ofertas, anuncios globales).
 * - POINT_TO_POINT: Notificación punto a punto dirigida exclusivamente a un usuario específico (confirmación de compra, reserva).
 */
public enum DeliveryMode {
    PUB_SUB,
    POINT_TO_POINT
}
