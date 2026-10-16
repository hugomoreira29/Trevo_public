package pt.trevo.trevo_api.dto;

import java.util.List;

public record ReviewPageResponse(
    Double averageRating,
    long totalElements,
    int totalPages,
    int page,
    int size,
    List<ReviewResponse> reviews
) {}
