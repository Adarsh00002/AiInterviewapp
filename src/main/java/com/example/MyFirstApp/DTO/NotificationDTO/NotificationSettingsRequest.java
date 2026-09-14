package com.example.MyFirstApp.DTO.NotificationDTO;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSettingsRequest {

    private Boolean streakReminders;

    private Boolean practiceReminders;

    private Boolean aiCoachRecommendations;

    private Boolean securityAlerts;

    private Boolean newFeaturesUpdates;

    private Boolean practiceProgressEmails;

    private Boolean productUpdatesEmail;
}