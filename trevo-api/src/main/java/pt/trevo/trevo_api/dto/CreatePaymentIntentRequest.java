package pt.trevo.trevo_api.dto;

import jakarta.validation.constraints.NotNull;

public record CreatePaymentIntentRequest(
    @NotNull Long orderId
) {
}
