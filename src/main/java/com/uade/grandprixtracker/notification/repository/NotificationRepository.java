package com.uade.grandprixtracker.notification.repository;

import com.uade.grandprixtracker.notification.model.Notification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Query("SELECT n FROM Notification n WHERE (:idUsuario IS NULL AND n.idUsuario IS NULL) OR (:idUsuario IS NOT NULL AND (n.idUsuario = :idUsuario OR n.idUsuario IS NULL)) ORDER BY n.creadoEn DESC")
    List<Notification> findAllForUser(@Param("idUsuario") UUID idUsuario);

    @Query("SELECT n FROM Notification n WHERE ((:idUsuario IS NULL AND n.idUsuario IS NULL) OR (:idUsuario IS NOT NULL AND (n.idUsuario = :idUsuario OR n.idUsuario IS NULL))) AND n.leido = false ORDER BY n.creadoEn DESC")
    List<Notification> findUnreadForUser(@Param("idUsuario") UUID idUsuario);

    @Query("SELECT COUNT(n) FROM Notification n WHERE ((:idUsuario IS NULL AND n.idUsuario IS NULL) OR (:idUsuario IS NOT NULL AND (n.idUsuario = :idUsuario OR n.idUsuario IS NULL))) AND n.leido = false")
    long countUnreadForUser(@Param("idUsuario") UUID idUsuario);

    @Modifying
    @Query("UPDATE Notification n SET n.leido = true WHERE n.idUsuario = :idUsuario AND n.leido = false")
    int markAllAsReadForUser(@Param("idUsuario") UUID idUsuario);
}
