package pt.trevo.trevo_api.dto;

import java.time.Instant;

public record NotificationResponse(
    Long id,
    Long orderId,
    String type,
    String title,
    String message,
    boolean read,
    Instant createdAt,
    Instant readAt
) {
}
