package com.example.MyFirstApp.service.NotificationService;

import com.example.MyFirstApp.DTO.NotificationDTO.NotificationResponse;
import com.example.MyFirstApp.Entity.Notification.Notification;
import com.example.MyFirstApp.Entity.Notification.NotificationSettings;
import com.example.MyFirstApp.repository.NotificationRepository.NotificationRepository;
import com.example.MyFirstApp.repository.NotificationRepository.NotificationSettingsRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    private final NotificationSettingsRepository settingsRepository;


    // =========================================================
    // GET USER NOTIFICATIONS
    // =========================================================

    public List<NotificationResponse> getNotifications(
            Long userId
    ) {

        return notificationRepository
                .findTop50ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }


    // =========================================================
    // UNREAD COUNT
    // =========================================================

    public long getUnreadCount(Long userId) {

        return notificationRepository
                .countByUserIdAndIsReadFalse(userId);
    }


    // =========================================================
    // MARK ONE AS READ
    // =========================================================

    @Transactional
    public void markAsRead(
            Long notificationId,
            Long userId
    ) {

        Notification notification =
                notificationRepository
                        .findById(notificationId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Notification not found"
                                )
                        );


        /*
         * User can modify only his own notification.
         */

        if (!notification.getUserId().equals(userId)) {

            throw new RuntimeException(
                    "Unauthorized notification access"
            );
        }


        notification.setIsRead(true);

        notificationRepository.save(notification);
    }


    // =========================================================
    // MARK ALL AS READ
    // =========================================================

    @Transactional
    public void markAllAsRead(Long userId) {

        List<Notification> notifications =
                notificationRepository
                        .findByUserIdOrderByCreatedAtDesc(userId);


        notifications.forEach(
                notification ->
                        notification.setIsRead(true)
        );


        notificationRepository.saveAll(notifications);
    }


    // =========================================================
    // CREATE NOTIFICATION
    // =========================================================

    @Transactional
    public Notification createNotification(
            Long userId,
            String type,
            String title,
            String message
    ) {

        return createNotification(
                userId,
                type,
                title,
                message,
                null,
                null
        );
    }


    // =========================================================
    // CREATE NOTIFICATION WITH ENTITY
    // =========================================================

    @Transactional
    public Notification createNotification(
            Long userId,
            String type,
            String title,
            String message,
            String entityType,
            Long entityId
    ) {

        /*
         * Basic validation
         */

        if (userId == null) {
            throw new IllegalArgumentException(
                    "User ID cannot be null"
            );
        }


        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException(
                    "Notification type cannot be empty"
            );
        }


        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                    "Notification title cannot be empty"
            );
        }


        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "Notification message cannot be empty"
            );
        }


        /*
         * Get user's notification settings.
         *
         * If settings don't exist, create default settings.
         */

        NotificationSettings settings =
                settingsRepository
                        .findByUserId(userId)
                        .orElseGet(
                                () -> createDefaultSettings(userId)
                        );


        /*
         * Check whether this notification
         * type is enabled.
         */

        if (!isNotificationAllowed(
                settings,
                type
        )) {

            return null;
        }


        Notification notification =
                Notification.builder()

                        .userId(userId)

                        .type(type)

                        .title(title)

                        .message(message)

                        .entityType(entityType)

                        .entityId(entityId)

                        .isRead(false)

                        .build();


        return notificationRepository.save(
                notification
        );
    }


    // =========================================================
    // STREAK NOTIFICATION
    // =========================================================
    //
    // Use after a successful practice/interview completion.
    //
    // Example:
    //
    // createStreakNotification(userId, 5);
    //
    // =========================================================

    @Transactional
    public Notification createStreakNotification(
            Long userId,
            int streakDays
    ) {

        if (userId == null) {
            throw new IllegalArgumentException(
                    "User ID cannot be null"
            );
        }


        if (streakDays <= 0) {
            return null;
        }


        /*
         * Don't create the same type of streak
         * notification multiple times in one day.
         */

        if (wasNotificationCreatedToday(
                userId,
                "STREAK_REMINDER"
        )) {

            return null;
        }


        String title;

        String message;


        if (streakDays == 1) {

            title =
                    "Your practice streak has started 🔥";

            message =
                    "Great start! Complete another practice tomorrow to keep your streak alive.";

        } else if (streakDays < 7) {

            title =
                    streakDays +
                            " day streak! 🔥";

            message =
                    "You're building a strong practice habit. Keep the momentum going.";

        } else {

            title =
                    "Amazing! " +
                            streakDays +
                            " day streak 🔥";

            message =
                    "Your consistency is paying off. Keep practicing and stay interview ready.";
        }


        return createNotification(
                userId,
                "STREAK_REMINDER",
                title,
                message
        );
    }


    // =========================================================
    // AI COACH RECOMMENDATION
    // =========================================================
    //
    // Use after interview evaluation when a weak area
    // has been identified.
    //
    // Example:
    //
    // createAiCoachRecommendation(
    //     userId,
    //     "Communication",
    //     58
    // );
    //
    // =========================================================

    @Transactional
    public Notification createAiCoachRecommendation(
            Long userId,
            String weakArea,
            double score
    ) {

        if (userId == null) {
            throw new IllegalArgumentException(
                    "User ID cannot be null"
            );
        }


        if (
                weakArea == null ||
                        weakArea.isBlank()
        ) {
            return null;
        }


        /*
         * Don't repeatedly send the same AI Coach
         * recommendation on the same day.
         */

        if (wasNotificationCreatedToday(
                userId,
                "AI_COACH"
        )) {

            return null;
        }


        double safeScore =
                Math.max(
                        0,
                        Math.min(
                                100,
                                score
                        )
                );


        String formattedScore =
                String.format(
                        "%.0f",
                        safeScore
                );


        String title =
                "Your AI Coach has a suggestion ✨";


        String message =
                "Your " +
                        weakArea.trim() +
                        " score is " +
                        formattedScore +
                        "%. Try a focused practice session to improve this area.";


        return createNotification(
                userId,
                "AI_COACH",
                title,
                message,
                "AI_COACH",
                null
        );
    }


    // =========================================================
    // PRODUCT UPDATE
    // =========================================================
    //
    // Example:
    //
    // createProductUpdate(
    //     userId,
    //     "New HR Interview Practice 🚀",
    //     "Practice realistic HR questions with AI."
    // );
    //
    // =========================================================

    @Transactional
    public Notification createProductUpdate(
            Long userId,
            String title,
            String message
    ) {

        if (userId == null) {
            throw new IllegalArgumentException(
                    "User ID cannot be null"
            );
        }


        if (
                title == null ||
                        title.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Product update title cannot be empty"
            );
        }


        if (
                message == null ||
                        message.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Product update message cannot be empty"
            );
        }


        return createNotification(
                userId,
                "PRODUCT_UPDATE",
                title,
                message
        );
    }


    // =========================================================
    // PRACTICE REMINDER
    // =========================================================
    //
    // This method only creates the notification.
    //
    // Scheduler decides WHEN to call it.
    //
    // =========================================================

    @Transactional
    public Notification createPracticeReminder(
            Long userId
    ) {

        if (userId == null) {
            throw new IllegalArgumentException(
                    "User ID cannot be null"
            );
        }


        /*
         * Prevent duplicate reminders on same day.
         */

        if (wasNotificationCreatedToday(
                userId,
                "PRACTICE_REMINDER"
        )) {

            return null;
        }


        return createNotification(
                userId,
                "PRACTICE_REMINDER",
                "Time to practice 🎯",
                "You haven't practiced today. A quick interview session can keep your preparation on track."
        );
    }


    // =========================================================
    // CHECK NOTIFICATION ALREADY CREATED TODAY
    // =========================================================

    private boolean wasNotificationCreatedToday(
            Long userId,
            String type
    ) {

        LocalDate today =
                LocalDate.now();


        LocalDateTime start =
                today.atStartOfDay();


        LocalDateTime end =
                today
                        .plusDays(1)
                        .atStartOfDay();


        return notificationRepository
                .existsByUserIdAndTypeAndCreatedAtBetween(
                        userId,
                        type,
                        start,
                        end
                );
    }


    // =========================================================
    // NOTIFICATION TYPE CHECK
    // =========================================================

    private boolean isNotificationAllowed(
            NotificationSettings settings,
            String type
    ) {

        if (settings == null) {
            return false;
        }


        if (type == null) {
            return false;
        }


        switch (
                type.trim().toUpperCase()
        ) {

            case "STREAK_REMINDER":

                return Boolean.TRUE.equals(
                        settings.getStreakReminders()
                );


            case "PRACTICE_REMINDER":

                return Boolean.TRUE.equals(
                        settings.getPracticeReminders()
                );


            case "AI_COACH":

                return Boolean.TRUE.equals(
                        settings.getAiCoachRecommendations()
                );


            case "SECURITY":

                /*
                 * Security alerts are ALWAYS enabled.
                 */

                return true;


            case "PRODUCT_UPDATE":

                return Boolean.TRUE.equals(
                        settings.getNewFeaturesUpdates()
                );


            default:

                /*
                 * Unknown notification types
                 * are blocked.
                 */

                return false;
        }
    }


    // =========================================================
    // DEFAULT SETTINGS
    // =========================================================

    private NotificationSettings createDefaultSettings(
            Long userId
    ) {

        NotificationSettings settings =
                NotificationSettings.builder()

                        .userId(userId)

                        .streakReminders(true)

                        .practiceReminders(true)

                        .aiCoachRecommendations(true)

                        .securityAlerts(true)

                        .newFeaturesUpdates(true)

                        .practiceProgressEmails(false)

                        .productUpdatesEmail(false)

                        .build();


        return settingsRepository.save(
                settings
        );
    }


    // =========================================================
    // DTO MAPPER
    // =========================================================

    private NotificationResponse toResponse(
            Notification notification
    ) {

        return NotificationResponse.builder()

                .id(
                        notification.getId()
                )

                .type(
                        notification.getType()
                )

                .title(
                        notification.getTitle()
                )

                .message(
                        notification.getMessage()
                )

                .entityType(
                        notification.getEntityType()
                )

                .entityId(
                        notification.getEntityId()
                )

                .isRead(
                        notification.getIsRead()
                )

                .createdAt(
                        notification.getCreatedAt()
                )

                .build();
    }
}