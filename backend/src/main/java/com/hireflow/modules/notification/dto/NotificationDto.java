package com.hireflow.modules.notification.dto;

import com.hireflow.modules.notification.NotificationPriority;
import com.hireflow.modules.notification.NotificationType;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private Long id;
    private Long notificationId;
    private String title;
    private String message;
    private NotificationPriority priority;
    private NotificationType type;
    private Long targetDriveId;
    private boolean read;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
