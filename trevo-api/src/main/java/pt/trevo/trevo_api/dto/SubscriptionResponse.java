package pt.trevo.trevo_api.dto;

import java.time.Instant;
import pt.trevo.trevo_api.persistence.entity.SubscriptionStatus;

public record SubscriptionResponse(
    Long id,
    SubscriptionStatus status,
    String stripeSubscriptionId,
    Instant startDate,
    Instant endDate,
    Instant createdAt
) {}
