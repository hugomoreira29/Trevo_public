package pt.trevo.trevo_api.service;

import java.time.Instant;

public record StripeSubscriptionResult(
    String id,
    String status,
    Instant currentPeriodStart,
    Instant currentPeriodEnd
) {}
