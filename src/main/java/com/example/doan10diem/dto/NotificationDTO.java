package com.example.doan10diem.dto;

import com.example.doan10diem.model.Notification;
import com.example.doan10diem.model.NotificationType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record NotificationDTO(
        Long id,
        String title,
        String message,
        String link,
        boolean read,
        String createdAtFormatted,
        String type
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static NotificationDTO from(Notification n) {
        String formatted = n.getCreatedAt() != null ? n.getCreatedAt().format(FORMATTER) : "";
        return new NotificationDTO(
                n.getId(),
                n.getTitle(),
                n.getMessage(),
                n.getLink(),
                n.isRead(),
                formatted,
                n.getType() != null ? n.getType().name() : ""
        );
    }
}
