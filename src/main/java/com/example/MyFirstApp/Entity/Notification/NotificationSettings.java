package com.example.MyFirstApp.Entity.Notification;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "notification_settings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notification_settings_user",
                        columnNames = "user_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /*
     * Practice & Progress
     */

    @Column(name = "streak_reminders", nullable = false)
    private Boolean streakReminders;

    @Column(name = "practice_reminders", nullable = false)
    private Boolean practiceReminders;

    @Column(name = "ai_coach_recommendations", nullable = false)
    private Boolean aiCoachRecommendations;

    /*
     * Security
     *
     * Always TRUE.
     * User cannot disable this.
     */

    @Column(name = "security_alerts", nullable = false)
    private Boolean securityAlerts;

    /*
     * Product
     */

    @Column(name = "new_features_updates", nullable = false)
    private Boolean newFeaturesUpdates;

    /*
     * Email
     */

    @Column(name = "practice_progress_emails", nullable = false)
    private Boolean practiceProgressEmails;

    @Column(name = "product_updates_email", nullable = false)
    private Boolean productUpdatesEmail;

    @PrePersist
    protected void onCreate() {

        if (streakReminders == null) {
            streakReminders = true;
        }

        if (practiceReminders == null) {
            practiceReminders = true;
        }

        if (aiCoachRecommendations == null) {
            aiCoachRecommendations = true;
        }

        // ALWAYS TRUE
        securityAlerts = true;

        if (newFeaturesUpdates == null) {
            newFeaturesUpdates = true;
        }

        if (practiceProgressEmails == null) {
            practiceProgressEmails = false;
        }

        if (productUpdatesEmail == null) {
            productUpdatesEmail = false;
        }
    }
}