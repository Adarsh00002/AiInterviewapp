package com.example.MyFirstApp.repository.NotificationRepository;


import com.example.MyFirstApp.Entity.Notification.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndIsReadFalse(Long userId);

    boolean existsByUserIdAndTypeAndCreatedAtBetween(
            Long userId,
            String type,
            java.time.LocalDateTime start,
            java.time.LocalDateTime end
    );
}