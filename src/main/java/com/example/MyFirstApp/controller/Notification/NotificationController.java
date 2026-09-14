package com.example.MyFirstApp.controller.Notification;

import com.example.MyFirstApp.DTO.NotificationDTO.NotificationResponse;
import com.example.MyFirstApp.DTO.NotificationDTO.NotificationSettingsRequest;
import com.example.MyFirstApp.DTO.NotificationDTO.NotificationSettingsResponse;
import com.example.MyFirstApp.Entity.Notification.Notification;
import com.example.MyFirstApp.service.NotificationService.NotificationService;
import com.example.MyFirstApp.service.NotificationService.NotificationSettingsService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    private final NotificationSettingsService notificationSettingsService;


    // =========================================================
    // GET NOTIFICATIONS
    // =========================================================

    @GetMapping("/{userId}")
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @PathVariable Long userId
    ) {

        return ResponseEntity.ok(
                notificationService.getNotifications(userId)
        );
    }


    // =========================================================
    // GET UNREAD COUNT
    // =========================================================

    @GetMapping("/{userId}/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @PathVariable Long userId
    ) {

        long count =
                notificationService.getUnreadCount(userId);


        Map<String, Long> response =
                new HashMap<>();

        response.put(
                "unreadCount",
                count
        );


        return ResponseEntity.ok(response);
    }


    // =========================================================
    // MARK SINGLE NOTIFICATION AS READ
    // =========================================================

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Map<String, String>> markAsRead(

            @PathVariable Long notificationId,

            @RequestParam Long userId

    ) {

        notificationService.markAsRead(
                notificationId,
                userId
        );


        Map<String, String> response =
                new HashMap<>();

        response.put(
                "message",
                "Notification marked as read"
        );


        return ResponseEntity.ok(response);
    }


    // =========================================================
    // MARK ALL AS READ
    // =========================================================

    @PatchMapping("/{userId}/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead(
            @PathVariable Long userId
    ) {

        notificationService.markAllAsRead(userId);


        Map<String, String> response =
                new HashMap<>();

        response.put(
                "message",
                "All notifications marked as read"
        );


        return ResponseEntity.ok(response);
    }


    // =========================================================
    // GET NOTIFICATION SETTINGS
    // =========================================================
    //
    // GET
    // /notifications/settings/{userId}
    //
    // =========================================================

    @GetMapping("/settings/{userId}")
    public ResponseEntity<NotificationSettingsResponse> getSettings(
            @PathVariable Long userId
    ) {

        return ResponseEntity.ok(
                notificationSettingsService.getSettings(
                        userId
                )
        );
    }


    // =========================================================
    // UPDATE NOTIFICATION SETTINGS
    // =========================================================
    //
    // PUT
    // /notifications/settings/{userId}
    //
    // =========================================================

    @PutMapping("/settings/{userId}")
    public ResponseEntity<NotificationSettingsResponse> updateSettings(

            @PathVariable Long userId,

            @RequestBody NotificationSettingsRequest request

    ) {

        return ResponseEntity.ok(
                notificationSettingsService.updateSettings(
                        userId,
                        request
                )
        );
    }
}