package pt.trevo.trevo_api.dto;

import jakarta.validation.constraints.NotNull;
import pt.trevo.trevo_api.persistence.entity.OrderStatus;

public record UpdateOrderStatusRequest(
    @NotNull OrderStatus status
) {}
