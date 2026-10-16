package pt.trevo.trevo_api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import pt.trevo.trevo_api.persistence.entity.PayoutStatus;

public record PayoutResponse(
    Long id,
    Long orderId,
    BigDecimal payoutAmount,
    PayoutStatus status,
    Instant createdAt,
    Instant releasedAt
) {
}
