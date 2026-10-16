package pt.trevo.trevo_api.dto;

import java.util.List;

public record CheckoutResponse(
    List<OrderResponse> orders,
    String approveUrl
) {}
