package pt.trevo.trevo_api.dto;

import java.util.List;

public record OrderPageResponse(
    long totalElements,
    int totalPages,
    int page,
    int size,
    List<OrderResponse> orders
) {}
