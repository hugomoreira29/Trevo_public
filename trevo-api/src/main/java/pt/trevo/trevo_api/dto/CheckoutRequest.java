package pt.trevo.trevo_api.dto;

import jakarta.validation.constraints.NotNull;
import pt.trevo.trevo_api.persistence.entity.PaymentMethod;

public record CheckoutRequest(
    @NotNull PaymentMethod paymentMethod,
    String phone
) {}
