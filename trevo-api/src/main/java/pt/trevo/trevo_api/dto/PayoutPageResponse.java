package pt.trevo.trevo_api.dto;

import java.util.List;

public record PayoutPageResponse(
    long totalElements,
    int totalPages,
    int page,
    int size,
    List<PayoutResponse> payouts
) {}
