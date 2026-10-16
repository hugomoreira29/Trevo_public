package pt.trevo.trevo_api.dto;

import java.time.Instant;

public record ReviewResponse(
    Long id,
    Long buyerId,
    String buyerName,
    Long shopId,
    Integer rating,
    String comment,
    String response,
    Instant createdAt,
    Instant updatedAt
) {}
