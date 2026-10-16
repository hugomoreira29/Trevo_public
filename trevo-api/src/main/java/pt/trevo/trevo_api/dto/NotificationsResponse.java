package pt.trevo.trevo_api.dto;

import java.util.List;

public record NotificationsResponse(
    long unreadCount,
    int page,
    int size,
    long totalElements,
    int totalPages,
    List<NotificationResponse> notifications
) {
}
