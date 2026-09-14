package com.example.MyFirstApp.repository.NotificationRepository;


import com.example.MyFirstApp.Entity.Notification.NotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationSettingsRepository
        extends JpaRepository<NotificationSettings, Long> {

    Optional<NotificationSettings> findByUserId(Long userId);
}