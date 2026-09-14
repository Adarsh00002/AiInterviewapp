package com.example.MyFirstApp.service.NotificationService;

import com.example.MyFirstApp.DTO.NotificationDTO.NotificationSettingsRequest;
import com.example.MyFirstApp.DTO.NotificationDTO.NotificationSettingsResponse;
import com.example.MyFirstApp.Entity.Notification.NotificationSettings;


import com.example.MyFirstApp.repository.NotificationRepository.NotificationSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationSettingsService {

    private final NotificationSettingsRepository repository;


    // =========================================================
    // GET SETTINGS
    // =========================================================

    @Transactional
    public NotificationSettingsResponse getSettings(Long userId) {

        NotificationSettings settings =
                repository.findByUserId(userId)
                        .orElseGet(
                                () -> createDefaultSettings(userId)
                        );

        return toResponse(settings);
    }


    // =========================================================
    // UPDATE SETTINGS
    // =========================================================

    @Transactional
    public NotificationSettingsResponse updateSettings(
            Long userId,
            NotificationSettingsRequest request
    ) {

        NotificationSettings settings =
                repository.findByUserId(userId)
                        .orElseGet(
                                () -> createDefaultSettings(userId)
                        );


        // =====================================================
        // STREAK REMINDERS
        // =====================================================

        if (request.getStreakReminders() != null) {

            settings.setStreakReminders(
                    request.getStreakReminders()
            );
        }


        // =====================================================
        // PRACTICE REMINDERS
        // =====================================================

        if (request.getPracticeReminders() != null) {

            settings.setPracticeReminders(
                    request.getPracticeReminders()
            );
        }


        // =====================================================
        // AI COACH RECOMMENDATIONS
        // =====================================================

        if (request.getAiCoachRecommendations() != null) {

            settings.setAiCoachRecommendations(
                    request.getAiCoachRecommendations()
            );
        }


        // =====================================================
        // SECURITY ALERTS
        // =====================================================
        //
        // Security alerts can never be disabled.
        //

        settings.setSecurityAlerts(true);


        // =====================================================
        // NEW FEATURES / UPDATES
        // =====================================================

        if (request.getNewFeaturesUpdates() != null) {

            settings.setNewFeaturesUpdates(
                    request.getNewFeaturesUpdates()
            );
        }


        // =====================================================
        // PRACTICE & PROGRESS EMAILS
        // =====================================================

        if (request.getPracticeProgressEmails() != null) {

            settings.setPracticeProgressEmails(
                    request.getPracticeProgressEmails()
            );
        }


        // =====================================================
        // PRODUCT UPDATES EMAIL
        // =====================================================

        if (request.getProductUpdatesEmail() != null) {

            settings.setProductUpdatesEmail(
                    request.getProductUpdatesEmail()
            );
        }


        // =====================================================
        // SAVE
        // =====================================================

        NotificationSettings saved =
                repository.save(settings);

        return toResponse(saved);
    }


    // =========================================================
    // CREATE DEFAULT SETTINGS
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


        return repository.save(settings);
    }


    // =========================================================
    // ENTITY -> RESPONSE DTO
    // =========================================================

    private NotificationSettingsResponse toResponse(
            NotificationSettings settings
    ) {

        return NotificationSettingsResponse.builder()

                .streakReminders(
                        settings.getStreakReminders()
                )

                .practiceReminders(
                        settings.getPracticeReminders()
                )

                .aiCoachRecommendations(
                        settings.getAiCoachRecommendations()
                )

                /*
                 * Always TRUE.
                 */
                .securityAlerts(true)

                .newFeaturesUpdates(
                        settings.getNewFeaturesUpdates()
                )

                .practiceProgressEmails(
                        settings.getPracticeProgressEmails()
                )

                .productUpdatesEmail(
                        settings.getProductUpdatesEmail()
                )

                .build();
    }
}